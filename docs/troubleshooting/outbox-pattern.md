# hhplus-concert — 아웃박스 패턴 도입

---

## 도입 배경 — 기존 결제 흐름의 문제

데이터 플랫폼 전송이 `AFTER_COMMIT` 이벤트 리스너에서 실패하면 그냥 유실됩니다.

```java
// 기존 ReservationEventListener
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    queueTokenService.expireToken(event.getToken());

    try {
        dataPlatformClient.sendReservationData(event.getPaymentId());
    } catch (Exception e) {
        log.error("데이터 플랫폼 API 호출 실패", e);  // 그냥 유실
    }
}
```

```
결제 성공 → AFTER_COMMIT → dataPlatformClient.send()
                                ↑ 실패하면 로그만 찍고 유실
                                ↑ 서버 다운되면 실행 자체가 안 됨
```

---

## 아웃박스 패턴이란?

이벤트 발행을 **DB 트랜잭션과 같이 묶어** 유실을 막는 패턴.

```
@Transactional
  → 비즈니스 로직 처리
  → outbox 테이블에 이벤트 저장  ← 같은 트랜잭션에 묶임
커밋 (원자적)

[별도 폴러 스케줄러]
  → outbox PENDING 이벤트 조회 → 처리 → PUBLISHED
```

커밋이 됐으면 outbox에 반드시 이벤트가 존재 → 폴러가 반드시 처리 보장

---

## 구성 요소

| 클래스 | 위치 | 역할 |
|--------|------|------|
| `OutboxEvent` | `common/outbox` | 이벤트 엔티티 (eventType, payload, status, failReason) |
| `OutboxEventType` | `common/outbox` | DATA_PLATFORM_SEND |
| `OutboxStatus` | `common/outbox` | PENDING / PUBLISHED / FAILED |
| `OutboxEventRepository` | `common/outbox` | PENDING 이벤트 조회, 오래된 이벤트 삭제 |
| `OutboxEventService.save()` | `common/outbox` | 기존 트랜잭션 참여 (결제 완료 시 같이 커밋) |
| `OutboxEventScheduler` | `common/outbox` | 10초마다 PENDING 처리, 매일 새벽 3시 오래된 이벤트 삭제 |

### DB 테이블

```sql
CREATE TABLE outbox_event (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type   VARCHAR(50)  NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME     NOT NULL,
    published_at DATETIME,
    fail_reason  TEXT          -- FAILED 시 원인 저장
);
```

`fail_reason`을 넣는 이유:
- `FAILED` 이벤트 수동 처리 시 로그를 뒤지지 않아도 원인 바로 파악 가능
- `e.getMessage()`를 저장해 어떤 예외로 실패했는지 DB에서 직접 확인

```
id | event_type          | status | fail_reason
---|---------------------|--------|---------------------------
3  | DATA_PLATFORM_SEND  | FAILED | Connection timeout
```

> FAILED 이벤트는 자동 삭제하지 않고 수동으로 원인 파악 후 재처리

---

## 변경 내용

### ReservationFacade.completePayment()

```
기존:
  - dataPlatformClient.send() → AFTER_COMMIT 즉시 호출 (유실 가능)
  - 결제 실패 시 보상 로직 없음

변경:
  - 성공 → outboxEventService.save(DATA_PLATFORM_SEND)  ← 트랜잭션 A와 같이 커밋
  - 실패 → balanceService.increase() 직접 호출  ← REQUIRES_NEW 독립 커밋
```

### ReservationEventListener

```
기존: AFTER_COMMIT → 토큰 만료 + dataPlatformClient.send() (실패 시 유실)
변경: AFTER_COMMIT → 토큰 만료 + dataPlatformClient.send() 즉시 시도
                     실패해도 아웃박스 폴러가 재처리 보장
```

### OutboxEventScheduler

```
기존: BALANCE_ROLLBACK → balanceService.increase()
변경: DATA_PLATFORM_SEND → dataPlatformClient.send()
```

---

## 정상 흐름

```
결제 성공
  → 트랜잭션 A
      reservationService.completeReserve()
      paymentService.processPayment()
      outboxEventService.save(DATA_PLATFORM_SEND, {paymentId})  ← 같이 커밋
  → 트랜잭션 A 커밋
        ↓ AFTER_COMMIT (ReservationEventListener)
  토큰 만료
  dataPlatformClient.send()  ← 즉시 전송 시도
    성공 → 끝
    실패 → 로그만 찍고 넘어감 (폴러가 보장)

[10초 후 폴러]
  → PENDING 조회 → DATA_PLATFORM_SEND 발견
  → dataPlatformClient.send(paymentId)  ← 중복 전송 or 재처리
  → PUBLISHED
```

---

## 결제 실패 흐름

```
balanceService.decrease()  → 트랜잭션 B 커밋 (잔액 차감 완료)

reservationService.completeReserve() 또는
paymentService.processPayment() 실패
        ↓ catch
balanceService.increase()  → REQUIRES_NEW 독립 커밋 (잔액 즉시 환불)
트랜잭션 A 롤백
```

잔액 보상은 아웃박스 없이 catch에서 직접 처리.
`increase()`도 `@DistributedLock` → `REQUIRES_NEW`라 트랜잭션 A 롤백과 무관하게 독립 커밋.

---

## 서버 장애 흐름

```
트랜잭션 A 커밋 성공
  → outbox: DATA_PLATFORM_SEND PENDING 저장됨
        ↓
서버 다운 → AFTER_COMMIT 유실 (토큰 만료 안 됨)
        ↓
서버 재시작
  → 토큰은 Redis TTL 10분 후 자동 만료
  → [10초 후 폴러] DATA_PLATFORM_SEND 재처리 → 전송 보장
```

---

## 데이터 적재 관리

```
삭제 주기:
  - 매일 새벽 3시 (cron: "0 0 3 * * *")
  - 7일 지난 PUBLISHED 이벤트 삭제
```

| 상태 | 보존 기간 |
|------|----------|
| PENDING | 처리 완료까지 |
| PUBLISHED | 7일 |
| FAILED | 수동 확인 후 처리 (자동 삭제 제외) |

---

## 핵심 요약

| 상황 | 기존 | 변경 후 |
|------|------|---------|
| 데이터 플랫폼 전송 실패 | 로그만 찍고 유실 | FAILED 저장 → 수동 재처리 |
| 서버 다운 후 재시작 | 전송 유실 | 폴러가 재처리 보장 |
| 잔액 차감 후 결제 실패 | 잔액 영구 손실 | catch에서 즉시 환불 |
| 토큰 만료 | AFTER_COMMIT 즉시 | AFTER_COMMIT + Redis TTL 보장 |

> 아웃박스 목적: 데이터 플랫폼 전송 이벤트를 트랜잭션과 원자적으로 묶어 유실 원천 차단
