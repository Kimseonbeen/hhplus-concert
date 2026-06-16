# Saga 패턴

## 개념

분산 트랜잭션을 여러 개의 로컬 트랜잭션으로 나누어 처리하는 패턴
각 단계가 실패하면 이전 단계를 되돌리는 보상 트랜잭션을 실행한다.

---

## 오케스트레이션 vs 코레오그래피

### 오케스트레이션 (현재 프로젝트 방식)

중앙 지휘자(Facade)가 모든 흐름을 직접 제어한다.

```
ReservationFacade
  → balanceService.decrease()
  → reservationService.complete()
  → paymentService.process()
  → 실패 시 직접 보상 호출
```

**장점:** 전체 흐름 파악 쉬움, 디버깅 용이  
**단점:** 지휘자 장애 시 전체 중단, 결합도 높음

---

### 코레오그래피 (Kafka 사가 — 향후 적용 예정)

중앙 지휘자 없이 각 서비스가 이벤트를 구독해 자율적으로 처리한다.

```
Facade → reservation-pending 발행 (시작 신호만)

BalanceConsumer
  → 잔액 차감
  → balance-decrease 발행

ReservationConsumer
  → 예약 완료
  → reservation-completed 발행

PaymentConsumer
  → 결제 처리
  → 끝
```

**장점:** 결합도 낮음, 서비스 독립적  
**단점:** 전체 흐름 파악 어려움, 디버깅 어려움

---

## 롤백 체인 (보상 트랜잭션)

실패 시 역방향으로 보상 이벤트를 발행한다.

```
PaymentConsumer 실패
  → reservation-rollback 발행

ReservationConsumer (rollback)
  → 예약 취소
  → balance-rollback 발행

BalanceConsumer (rollback)
  → 잔액 환불
  → 끝
```

---

## 현재 프로젝트 적용 현황

| 항목 | 상태 |
|---|---|
| 오케스트레이션 (ReservationFacade) | ✓ 적용 |
| 아웃박스 패턴 (이벤트 유실 방지) | ✓ 적용 |
| 코레오그래피 (Kafka 사가) | 향후 적용 예정 |

### 현재 보상 트랜잭션 방식

잔액 차감은 `REQUIRES_NEW`로 독립 트랜잭션 커밋되므로,
결제 실패 시 `catch`에서 직접 보상 처리 (베스트 에포트).

```java
} catch (Exception e) {
    balanceService.increase(command.userId(), amount); // 직접 보상
    throw e;
}
```

완전한 보상 트랜잭션은 Kafka 코레오그래피로 추후 구현 예정.
