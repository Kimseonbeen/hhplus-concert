# HHPlus Concert 프로젝트 가이드

## 전체 예약 프로세스

```
1. 토큰 발급        → 대기열 진입
2. 잔액 충전        → 결제 준비
3. 스케줄 조회      → 예약 가능 날짜 확인
4. 좌석 조회        → 예약 가능 좌석 확인
5. 좌석 예약        → 5분 내 결제 필요 (expiredAt)
6. 결제             → 예약 확정, 토큰 만료
```

---

## API 목록

### 1. 토큰 발급
```
POST /api/queue-token
```

**요청**
```json
{
  "userId": 1
}
```

**응답**
```json
{
  "token": "uuid-string",
  "status": "WAITING",   // WAITING | ACTIVE
  "num": 5,              // 대기 순번 (ACTIVE면 0)
  "expiredAt": null      // ACTIVE면 만료시간 설정
}
```

> 이후 모든 예약/결제 API는 헤더에 `TOKEN: {token}` 필요

---

### 2. 잔액 조회
```
GET /api/balance/{userId}
```

**응답**
```json
{
  "userId": 1,
  "amount": 100000
}
```

---

### 3. 잔액 충전
```
POST /api/balance/{userId}/charge
```

**요청**
```json
{
  "amount": 50000
}
```

**응답**
```json
{
  "userId": 1,
  "amount": 150000
}
```

---

### 4. 예약 가능 날짜 조회
```
GET /api/concert/{concertId}/schedules
```

**응답**
```json
[
  {
    "concertScheduleId": 1,
    "concertDate": "2026-03-01T18:00:00"
  },
  {
    "concertScheduleId": 2,
    "concertDate": "2026-03-08T18:00:00"
  }
]
```

---

### 5. 예약 가능 좌석 조회
```
GET /api/concert/{concertScheduleId}/seats
```

**응답**
```json
{
  "date": "2026-03-01T18:00:00",
  "availableSeats": [1, 2, 3, 5, 7]  // 예약 가능한 좌석 번호 목록
}
```

---

### 6. 좌석 예약
```
POST /api/reservation/reserve
Header: TOKEN: {token}
```

**요청**
```json
{
  "scheduleId": 1,
  "seatId": 5,
  "userId": 1,
  "date": "2026-03-01T18:00:00",
  "seatNum": 15
}
```

> ⚠️ `date`, `seatNum`은 현재 사용되지 않음 (toCommand()에서 제외)

**응답**
```json
{
  "reservationId": 42,
  "concertId": 1,
  "concertAt": "2026-03-01T18:00:00",
  "seat": {
    "seatId": 5,
    "seatNo": 15,
    "seatPrice": 50000
  },
  "reservationStatus": "PENDING_PAYMENT",
  "expiredAt": "2026-02-20T15:05:00"   // 5분 후 만료
}
```

> `reservationId`는 결제 요청 시 필요 — 반드시 저장

---

### 7. 결제
```
POST /api/reservation/payment
Header: TOKEN: {token}
```

**요청**
```json
{
  "reservationId": 42,
  "userId": 1,
  "amount": 50000
}
```

**응답**
```json
{
  "paymentId": 10,
  "userId": 1,
  "reservationId": 42,
  "amount": 50000
}
```

> 결제 완료 시 토큰 만료, 예약 상태 `CONFIRMED`로 변경

---

## 도메인 흐름

### 좌석 예약 내부 흐름
```
ReservationController.createReserve()
  → ReservationFacade.reserve()
    → ConcertService.reserveSeat()        // 좌석 상태 RESERVED로 변경
      → ConcertScheduleRepository 조회    // concertId, concertDate 획득
    → ReservationService.createReservation()  // Reservation 저장 (PENDING_PAYMENT, expiredAt=+5분)
    → ReservationResult 반환
  → ReservationResponse.from()
```

### 결제 내부 흐름
```
ReservationController.createPayment()
  → ReservationFacade.completePayment()
    → BalanceService.decrease()           // 잔액 차감
    → ReservationService.completeReserve() // 예약 상태 CONFIRMED로 변경
    → PaymentService.processPayment()     // 결제 내역 저장
    → QueueTokenService.expireToken()     // 토큰 만료
    → PaymentCompletedEvent 발행          // 데이터 플랫폼 전송
  → PaymentResponse 반환
```

---

## 대기열 토큰 구조

```
WAITING  →  Redis ZSET (waiting-token)
              멤버: token, 점수: 타임스탬프 (선착순)

ACTIVE   →  Redis String (active-token:{token})
              TTL: 10분
```

- 스케줄러가 **30초마다** 최대 150명까지 WAITING → ACTIVE 전환
- Lua 스크립트로 원자적 처리

---

## 예약 상태

| 상태 | 설명 |
|------|------|
| `PENDING_PAYMENT` | 예약 완료, 결제 대기 중 (5분 내 결제 필요) |
| `CONFIRMED` | 결제 완료, 예약 확정 |

## 좌석 상태

| 상태 | 설명 |
|------|------|
| `AVAILABLE` | 예약 가능 |
| `RESERVED` | 예약됨 |

---

## 코드리뷰 수정 내역

### ✅ 완료

#### 1. ReservationResponse 필드 매핑 완성
- **파일**: `ReservationResult`, `SeatResult`, `ConcertService`, `ReservationFacade`, `ReservationResponse`
- **문제**: `ReservationResponse.from()`이 비어있어 모든 필드 null 반환
- **수정**:
  - `ConcertService.reserveSeat()` — `ConcertSchedule` 추가 조회
  - `SeatResult.from(Seat, ConcertSchedule)` — 전체 필드 매핑
  - `ReservationResult` — `reservationId`, `seatNum`, `concertId`, `concertAt`, `status`, `expiredAt` 추가
  - `ReservationFacade.reserve()` — `createReservation()` 반환값 활용
  - `ReservationResponse.from()` — 전체 필드 매핑 완성

---

### 🔴 미완료 (즉시)

#### 2. 중복 이벤트 리스너
- **파일**: `PaymentEventListener`, `ReservationEventListener`
- **문제**: 동일한 `PaymentCompletedEvent`를 두 리스너가 구독 → 데이터 플랫폼 API 2번 호출

#### 3. PaymentConsumer_test.java 위치 오류
- **파일**: `src/main/java/.../reservation/application/PaymentConsumer_test.java`
- **문제**: `src/main`에 있어서 운영 환경에서도 Kafka 리스너 등록됨

---

### 🟡 미완료 (단기)

#### 4. OptimisticLockException 미처리
- **파일**: `ReservationFacade.reserve()`
- **문제**: 좌석 동시 예약 시 500 에러 반환 → 409 Conflict로 처리 필요

#### 5. 분산락 실패 예외 타입
- **파일**: `DistributedLockAop`
- **문제**: `IllegalStateException` → 커스텀 예외 + 429 반환 필요

#### 6. 토큰 삭제 타이밍
- **파일**: `ReservationFacade.completePayment()`
- **문제**: 트랜잭션 커밋 전에 토큰 삭제 → `AFTER_COMMIT`으로 변경 필요

#### 7. 예약 만료 스케줄러 없음
- **문제**: `expiredAt` 설정은 있지만 만료 처리 없음 → 좌석이 영원히 `RESERVED` 상태

---

### 🟢 미완료 (코드 품질)

- 예외 클래스명 통일 (`BalanceError` → `BalanceException`)
- 미사용 필드 제거 (`BalanceService`의 `KafkaTemplate`)
- 불필요한 이벤트 재생성 제거 (`ReservationCreatedEventListener`)

---

## ⚠️ 알려진 이슈

### 통합 테스트 깨질 가능성
- **파일**: `ConcurrentReservationIntegrationTest`
- **원인**: `BeforeEach`에서 `ConcertSchedule` 저장 없이 `Concert.id`를 `concertScheduleId`로 사용
- **영향**: `ConcertService.reserveSeat()` 수정으로 `ConcertSchedule` 조회 추가됨 → 테스트 실패 예상
- **해결**: `BeforeEach`에 `ConcertSchedule` 저장 추가 필요
