# MSA 환경에서 트랜잭션 분리 및 처리 방안

## 1. 개요
서비스의 규모가 확장됨에 따라 MSA(Microservices Architecture) 형태로 각 도메인별 배포 단위를 분리해야 한다. 하지만 MSA 환경에서는 기존의 단일 서비스 내에서 동작하던 트랜잭션이 여러 서비스로 나뉘게 되므로 트랜잭션 처리의 한계가 발생할 수 있다.

본 문서에서는 트랜잭션 분리 후 `@TransactionalEventListener`를 활용하여 문제를 해결하는 방안을 설명하고, 보상 트랜잭션 및 이벤트 체인을 적용하여 순서를 보장하는 방법을 제시한다.

---

## 2. 기존 트랜잭션 문제점

### 2.1 기존 코드 (단일 서비스 환경)
```java
public class ReservationFacade {

    @Transactional
    void 좌석예약() {
        콘서트서비스.좌석예약();
        예약서비스.예약생성();
    }
    
    @Transactional
    void 예약결제() {
        잔액서비스.잔액감소();
        예약서비스.예약완료();
        결제서비스.결제생성();
        토큰서비스.만료처리();
    }
}
```

### 2.2 문제점
MSA로 각 도메인을 분리할 경우, 위와 같은 트랜잭션은 한 서비스 내에서만 유효하며, 여러 서비스 간의 원자성이 보장되지 않는다. 따라서 하나의 서비스에서 장애가 발생하면 전체 트랜잭션이 롤백되지 않고 일부만 적용되는 문제가 발생할 수 있다.

---

## 3. 트랜잭션 분리 및 `@TransactionalEventListener` 활용

### 3.1 트랜잭션 분리
각 도메인 서비스에서 개별적으로 트랜잭션을 수행하도록 변경한다.
```java
// 잔액서비스
@Transactional
void 잔액감소() {
    잔액감소();
}

// 예약서비스
@Transactional
void 예약완료() {
    예약상태변경();
}

// 결제서비스
@Transactional
void 결제() {
    결제생성();
}

// 토큰서비스
@Transactional
void 토큰만료() {
    토큰삭제();
}
```

### 3.2 `@TransactionalEventListener` 활용
이벤트 기반으로 트랜잭션을 연결하여, 각 서비스 간의 결합도를 낮추면서도 트랜잭션을 처리한다.

```java
// 결제서비스
@Transactional
void 결제() {
   결제생성();
   이벤트발행(new PaymentCreatedEvent());  // 결제완료 이벤트 발행
}

// 잔액서비스
@TransactionalEventListener
@Transactional
void 잔액감소(PaymentCreatedEvent event) {  // 결제완료 이벤트 수신
  잔액감소();
}

// 예약서비스
@TransactionalEventListener
@Transactional
void 예약완료(PaymentCreatedEvent event) {  // 결제완료 이벤트 수신  
    예약완료처리();
}

// 토큰서비스
@TransactionalEventListener
@Transactional
void 토큰만료(PaymentCreatedEvent event) {  // 결제완료 이벤트 수신
    토큰만료처리();
}
```

### 3.3 문제점
1. **트랜잭션 실패 시 불완전한 상태 발생**
    - 예: `예약완료()`가 실패하면, 이미 `잔액감소()`와 `결제()`가 완료된 상태가 된다.
    - 일부 서비스에서 장애가 발생해도 다른 서비스의 데이터는 롤백되지 않아 데이터 불일치가 발생한다.
2. **이벤트 순서 보장 문제**
    - 현재 PaymentCreatedEvent 하나의 이벤트를 여러 서비스가 구독하고 있어 `@TransactionalEventListener`들은 병렬로 실행될 수 있으며 예상한 순서대로 실행되지 않을 수 있다.
    - 예를 들어 `토큰만료()`가 `잔액감소()`보다 먼저 실행될 경우, 토큰이 먼저 만료되는 문제가 발생할 수 있다.
---

## 4. 보상 트랜잭션 적용
트랜잭션 실패 시 보상 트랜잭션을 적용하여 상태를 복구한다.

```java
// 결제서비스
@Transactional
void 결제() {
  결제생성();  // 상태: PENDING
  이벤트발행(new PaymentCreatedEvent());
}

// 잔액서비스
@TransactionalEventListener
void 잔액감소(PaymentCreatedEvent event) {
  try {
    잔액감소();  // 100,000 -> 20,000
    이벤트발행(new BalanceDecreasedEvent());
  } catch(Exception e) {
    이벤트발행(new PaymentCompensationEvent());
  }
}

// 예약서비스
@TransactionalEventListener
void 예약완료(BalanceDecreasedEvent event) {
  try {
    예약완료();
    이벤트발행(new ReservationCompletedEvent());
  } catch(Exception e) {
    이벤트발행(new BalanceCompensationEvent());
    이벤트발행(new PaymentCompensationEvent());
  }
}

// 보상 트랜잭션 처리
@TransactionalEventListener
void 잔액원복(BalanceCompensationEvent event) {
  잔액증가();
}

@TransactionalEventListener
void 결제취소(PaymentCompensationEvent event) {
  결제취소처리();
}
```

### 문제점
이런식으로 보상트랜잭션을 적용하여 문제를 해결한 듯 보이지만, 여기에는 아직 큰 문제가 남아있다.  
현재 코드의 문제는 모든 서비스가 PaymentCreatedEvent를 동시에 구독하고 있어서, 어떤 서비스가 먼저 실행될지 보장할 수 없다는 것이다.
```markdown
결제() -> PaymentCreatedEvent 발행
           ↓
     잔액감소()  \  
     예약완료()   → 순서 보장 X (두 서비스가 동시에 이벤트를 받을 수 있음)
```

---

## 5. 이벤트 체인 적용 (순서 보장)
이벤트 체인을 활용하여 순차적으로 이벤트를 실행하여 순서를 보장한다.

```markdown
결제완료 -> 잔액감소완료 -> 예약완료
(PaymentCreatedEvent -> BalanceDecreasedEvent -> ReservationCompletedEvent)

실패 시: 역순으로 보상 트랜잭션 실행
예약실패 -> 잔액원복 -> 결제취소
```

```java
// 결제서비스
@Transactional
void 결제() {
    결제생성();  // 상태: PENDING
    이벤트발행(PaymentCreatedEvent);
}

// 잔액서비스
@TransactionalEventListener
void 잔액감소(PaymentCreatedEvent event) {  // 결제 이벤트 수신
    try {
        잔액감소();  // 100,000 -> 20,000
        이벤트발행(BalanceDecreasedEvent);  // 잔액감소 완료 이벤트 발행
    } catch(Exception e) {
        이벤트발행(PaymentCompensationEvent);
    }
}

// 예약서비스
@TransactionalEventListener
void 예약완료(BalanceDecreasedEvent event) {  // 잔액감소 완료 이벤트 수신
    try {
        예약완료();
        이벤트발행(ReservationCompletedEvent);  // 예약완료 이벤트 발행
    } catch(Exception e) {
        이벤트발행(BalanceCompensationEvent);
        이벤트발행(PaymentCompensationEvent);
    }
}

// 토큰서비스
@TransactionalEventListener
void 토큰만료(ReservationCompletedEvent event) {  // 예약완료 이벤트 수신
    try {
        토큰만료();
    } catch(Exception e) {
        이벤트발행(ReservationCompensationEvent);
        이벤트발행(BalanceCompensationEvent);
        이벤트발행(PaymentCompensationEvent);
    }
}

// 보상 트랜잭션 처리
@TransactionalEventListener
void 예약원복(BalanceCompensationEvent event) {
    예약상태변경처리();
}

@TransactionalEventListener
void 잔액원복(BalanceCompensationEvent event) {
    잔액증가();
}

@TransactionalEventListener
void 결제취소(PaymentCompensationEvent event) {
    결제상태변경처리();
}
```
---
### 5.1 이벤트 체인 패턴 적용 주요 변경점

### 1. 순차적 이벤트 체인 구성

#### 이전
- 모든 서비스가 `PaymentCreatedEvent`를 구독

#### 변경
- 각 서비스가 이전 단계의 이벤트만 구독

>결제 -> PaymentCreatedEvent -> 잔액감소 -> BalanceDecreasedEvent -> 예약완료 -> ReservationCompletedEvent -> 토큰만료


### 2. 각 서비스의 이벤트 수신 변경

- **잔액 서비스**: `PaymentCreatedEvent` 수신 (유지)
- **예약 서비스**: `PaymentCreatedEvent` → `BalanceDecreasedEvent`로 변경
- **토큰 서비스**: `PaymentCreatedEvent` → `ReservationCompletedEvent`로 변경

### 3. 보상 트랜잭션 흐름 정의
> 토큰만료 실패 -> 예약원복 -> 잔액원복 -> 결제취소


### 4. 각 단계별 실패 처리

- 각 서비스는 실패 시 이전 단계들의 보상 이벤트를 발행
- 보상 트랜잭션도 순차적으로 실행되도록 보장

### 5. 다른 해결 방안 고려

- 이벤트 마다 체인을 연결하는 형태가 아닌 @Order를 사용해 순서를 보장

---

### 결과
✅ 순차적 실행이 보장됨  
✅ 실패 시 역순으로 보상 처리가 진행됨  
✅ 데이터 일관성이 유지됨

---

### 6. SAGA 패턴의 이해

문제를 하나씩 해결하는 과정에서 SAGA 패턴 중 PHONE TAG SAGA 패턴에 도달하게 되었다.

#### 6.1 SAGA 패턴이란?
Saga 패턴은 여러 개의 서비스가 하나의 워크플로우를 만드는 분산 워크플로우를 처리하기 위한 패턴이다. 다음과 같은 선택지를 통해 패턴을 결정할 수 있다

- 분산 워크플로우를 하나의 트랜잭션인 원자성으로 처리할지, 최종 일관성으로 느슨하게 유지할지
- 동기 통신을 사용할지, 비동기 통신을 사용할지
- 중앙 중재자가 있는 오케스트레이션 방식을 사용할지, 서비스들 간의 협업을 통해 처리하는 코레오그래피 방식을 사용할지

#### 6.2 SAGA 패턴의 두 가지 방식

1. **오케스트레이션 사가(Orchestration saga)**
- 이벤트 교환, 동기화를 중앙화해서 처리
- 별도의 중재자 서버가 존재

2. **코레오그래피 사가(Choreography Saga)**
- 이벤트 교환, 동기화를 참가자에 처리를 맡김
- 워크플로우에 맞춰 체인 형태로 서비스가 이어져 있음

#### 6.3 PHONE TAG SAGA의 특징
동기 통신 + 원자적 일관성 + 코레오그래피의 특징을 가진다.

**구조적 특징:**
- 폰 태그(전화 옮겨 말하기)라는 이름처럼 서비스 간의 워크플로우가 체인처럼 연결
- 최초로 트랜잭션을 시작한 서비스가 조정점(coordination point)이 됨
- 최초 트랜잭션 시작 서비스를 프런트 컨트롤러라고도 함
- 각 서비스는 자신의 트랜잭션이 실패할 경우 책임 체인을 통해 상위 워크플로우에 보상 트랜잭션을 보내고, 상위 워크플로우는 기존 작업을 undo

**주요 특성**  
- 높은 커플링 (동기 + 책임 체인)
- 낮은 확장성 (동기 + 원자성)
- 낮은 가용성 (동기 + 원자성)
- 높은 복잡도 (코레오그래피)

**적용 시나리오**  
코레오그래피 방식의 SAGA 패턴이지만, 동기 통신과 원자성을 사용하기 때문에 확장성과 가용성이 제한적이다. 따라서 이 패턴은 워크플로우가 단순하고 에러 발생 가능성이 낮은 트랜잭션에 적합하다.


### 7. 결론

- MSA 환경에서 분산 트랜잭션의 한계를 확인하고, 이를 PHONE TAG 패턴으로 해결해보았습니다.
- 순차적 실행과 데이터 일관성은 보장되지만, 각 단계가 이전 단계의 완료를 기다려야 하는 동기 방식의 한계가 있습니다.
- 추가 개선 방안으로는
  - Outbox 패턴을 통한 이벤트 발행과 데이터베이스 트랜잭션의 일관성 보장  
  - 비동기 메시징 큐(Kafka, RabbitMQ)를 활용한 성능 개선   
  - Eventual Consistency를 고려한 설계로 전환  
  - Saga 패턴의 다양한 구현 방식(Choreography) 검토 등을 고려가 있습니다.