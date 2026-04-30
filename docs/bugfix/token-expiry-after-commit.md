# 토큰 만료 타이밍 버그

## 문제

`ReservationFacade.completePayment()` 내에서 DB 커밋 전에 Redis 토큰을 삭제하는 문제.

결제 처리 중 예외 발생 시 DB는 롤백되지만, Redis 토큰은 이미 삭제된 상태로 남아
사용자가 재시도를 해도 토큰이 없어서 요청이 불가능한 상황이 발생한다.

## 기존 코드

```java
@Transactional
public PaymentResult completePayment(PaymentCommand command, String token) {
    balanceService.decrease(...);            // DB write
    reservationService.completeReserve(...); // DB write
    paymentService.processPayment(...);      // DB write

    queueTokenService.expireToken(token);    // ← Redis 삭제 (아직 DB 커밋 전)

    eventPublisher.publishEvent(new PaymentCompletedEvent(payment.getId()));
    // ↑ 여기서 예외 발생 시 → DB 롤백 O, Redis 토큰 삭제 복구 X
}
```

## 원인

`queueTokenService.expireToken()`은 Redis를 직접 수정하는 연산으로, Spring 트랜잭션 범위 밖에 있음.
트랜잭션이 롤백되어도 Redis 변경은 되돌아오지 않는다.

### @Transactional 안에 있는데 왜 Redis는 범위 밖인가?

Spring의 `@Transactional`은 **DB 트랜잭션만** 관리한다.
내부적으로 `JpaTransactionManager`가 동작하며, **JDBC 커넥션의 commit/rollback만** 제어한다.

```
@Transactional
 └── JpaTransactionManager
      └── JDBC Connection (commit / rollback 가능)
           ├── balanceService.decrease()    → DB write ✅ 롤백 가능
           ├── reservationService...        → DB write ✅ 롤백 가능
           └── paymentService...            → DB write ✅ 롤백 가능
```

Redis는 완전히 **별도의 서버**이고, `JpaTransactionManager`가 알지 못한다.
Redis 명령어는 **호출 즉시 실행**되며 트랜잭션 매니저가 추적하지 않는다.

```
queueTokenService.expireToken(token)
 └── redisTemplate.delete(key)  → Redis 서버에 즉시 명령 전송
                                   트랜잭션 매니저가 추적 안 함
                                   → 롤백 메커니즘 없음
```

즉, `@Transactional` 메서드 안에 있더라도 Redis 명령어는 **호출 즉시 실행**되고 롤백이 불가능하다.

### Redis를 트랜잭션에 포함시킬 수 없나?

`redisTemplate.setEnableTransactionSupport(true)` 설정으로 DB 트랜잭션과 묶을 수 있지만,
Redis의 MULTI/EXEC 방식은 **롤백이 없어** 완전한 원자성 보장이 안 된다.
따라서 일반적으로 **Redis는 트랜잭션에 묶지 않고, AFTER_COMMIT으로 타이밍을 조절**하는 방식을 사용한다.

## 해결 방법

`@TransactionalEventListener(phase = AFTER_COMMIT)`을 활용해 DB 커밋이 완전히 성공한 후에만 토큰을 만료시킨다.

```
Before:
  DB write → expireToken() [Redis 즉시 삭제] → DB 커밋
  → DB 롤백 시 Redis 토큰은 이미 삭제됨 ❌

After:
  DB write → DB 커밋 완료 → AFTER_COMMIT 이벤트 → expireToken() [Redis]
  → DB 롤백 시 이벤트 자체가 실행되지 않음 ✅
```

## 수정 내용

### 1. PaymentCompletedEvent - token 필드 추가

```java
@AllArgsConstructor
@Getter
public class PaymentCompletedEvent {
    private final Long paymentId;
    private final String token; // 추가
}
```

### 2. ReservationFacade - 직접 호출 제거

```java
@Transactional
public PaymentResult completePayment(PaymentCommand command, String token) {
    ...
    // 제거: queueTokenService.expireToken(token);

    // token을 이벤트에 포함해서 발행
    eventPublisher.publishEvent(new PaymentCompletedEvent(payment.getId(), token));
}
```

### 3. ReservationEventListener - AFTER_COMMIT에서 처리

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    // DB 커밋 성공 후에만 실행됨
    queueTokenService.expireToken(event.getToken());
    dataPlatformClient.sendReservationData(event.getPaymentId());
}
```

## @TransactionalEventListener 동작 원리

| Phase | 실행 시점 |
|---|---|
| `BEFORE_COMMIT` | 커밋 직전 (트랜잭션 안) |
| `AFTER_COMMIT` | 커밋 성공 후 |
| `AFTER_ROLLBACK` | 롤백 후 |

`AFTER_COMMIT`을 사용하면 트랜잭션이 성공적으로 커밋된 경우에만 리스너가 실행된다.
DB 롤백 시 이 메서드 자체가 호출되지 않으므로 토큰이 보존된다.

## 변경 파일

| 파일 | 변경 내용 |
|---|---|
| `reservation/domain/event/PaymentCompletedEvent.java` | `token` 필드 추가 |
| `reservation/application/ReservationFacade.java` | 직접 `expireToken()` 제거, 이벤트에 token 포함, `QueueTokenService` 의존성 제거 |
| `reservation/application/event/ReservationEventListener.java` | `QueueTokenService` 주입, AFTER_COMMIT 핸들러에서 `expireToken()` 호출 |
| `reservation/application/ReservationFacadeTest.java` | 토큰 만료 검증을 이벤트 발행 검증으로 변경 |
