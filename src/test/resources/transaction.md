### **`STEP 16_심화`**
- 서비스의 규모가 확장되어 MSA의 형태로 각 도메인별로 배포단위를 분리해야한다면
  그 분리에 따른 트랜잭션 처리의 한계와 해결방안에 대한 서비스 설계 문서 작성
- 실시간 주문(이커머스), 좌석예약 정보(콘서트)를 데이터 플랫폼에 전달(mock API 호출)하는
  요구사항 등을 기존 로직에 추가해 보고 기존 로직에 영향을 주지 않도록 개선

( Try if you want )

- Facade 활용한다면, 트랜잭션을 도메인 단위로 분리하고, 발생하는 분산 트랜잭션 문제 처리하기
- Facade 없이 서비스간 의존하는 구조라면, 어플리케이션 이벤트를 활용하여 각 서비스 의존을 없애기

- 보상트랜잭션, Saga 패턴 등 활용

---
- 서비스의 규모가 확장되어 MSA의 형태로 각 도메인별로 배포단위를 분리해야한다면
  그 분리에 따른 트랜잭션 처리의 한계와 해결방안에 대한 서비스 설계 문서 작성  

[1] 트랜잭션을 나눠볼 수 있는 기능을 찾기  

ReservationFacade.class

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
- msa 형태로 각 도메인별 배포단위를 분리해야한다면 이 로직은 동작하지 않는다
- 왜 ?? 이유 작성

그럼 우리는 예로 예약결제에 대해 트랜잭션을 분리 이후 이번에 배운 키워드 @TransactionalEventListener를 활용해보자
- @TransactionalEventListener가 뭔데 ?
- 뭔지 작성

1. 트랙잭션분리
````java
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
````

2. @TransactionalEventListener를 활용
```java
// 결제서비스
@Transactional
void 결제() {
   결제생성();
   이벤트발행(PaymentCreatedEvent);  // 결제완료 이벤트 발행
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
순서:
결제서비스 → PaymentCreatedEvent 발행 → 잔액/예약/토큰 서비스가 이벤트 수신  
문제점:
1. 1단계: 결제 완료 (성공)
  - DB 결제상태: COMPLETED

2단계: 잔액 감소 (성공)
- DB 잔액: 20,000원

3단계: 예약 완료 (실패!)
- 에러 발생
- 하지만 결제는 완료상태
- 잔액도 이미 차감됨

2.모든 서비스가 동일한 PaymentCreatedEvent를 구독
@TransactionalEventListener들이 병렬로 실행될 수 있어서 순서 보장이 안됨
예를 들어:

토큰만료가 잔액감소보다 먼저 실행될 수 있음
예약완료가 잔액감소 전에 실행될 수 있음

그럼 우선 트랜잭션 실패 케이스 부터 해결해보자
- 보상 트랜잭션 적용
```java
// 결제서비스
@Transactional
void 결제() {
  결제생성();  // 상태: PENDING
  이벤트발행(PaymentCreatedEvent);
}

// 잔액서비스
@TransactionalEventListener
void 잔액감소(PaymentCreatedEvent event) {
  try {
    잔액감소();  // 100,000 -> 20,000
    이벤트발행(BalanceDecreasedEvent);
  } catch(Exception e) {
    // 결제 취소 이벤트 발행
    이벤트발행(PaymentCompensationEvent);
  }
}

// 예약서비스
@TransactionalEventListener
void 예약완료(BalanceDecreasedEvent event) {
  try {
    예약완료();
    이벤트발행(ReservationCompletedEvent);
  } catch(Exception e) {
    // 잔액 원복 이벤트 발행
    이벤트발행(BalanceCompensationEvent);
    // 결제 취소 이벤트 발행
    이벤트발행(PaymentCompensationEvent);
  }
}

// 보상 트랜잭션 처리
@TransactionalEventListener
void 잔액원복(BalanceCompensationEvent event) {
  잔액증가();  // 20,000 -> 100,000
}

@TransactionalEventListener
void 결제취소(PaymentCompensationEvent event) {
  결제취소처리();  // 상태: PENDING -> CANCELED
}
```
이런식으로 보상트랜잭션을 적용하여 문제를 해결한 듯 보이지만, 여기에는 아직 큰 문제가 남아있다.  
현재 코드의 문제는 모든 서비스가 PaymentCreatedEvent를 동시에 구독하고 있어서, 어떤 서비스가 먼저 실행될지 보장할 수 없다는 것이다.
```markdown
결제() -> PaymentCreatedEvent 발행
           ↓
     잔액감소()  \  
     예약완료()   → 순서 보장 X (두 서비스가 동시에 이벤트를 받을 수 있음)
```
그래서

PaymentCreatedEvent 발행  
예약완료() 먼저 실행 (성공)  
그 후 잔액감소() 실행 (실패)  
결과: 잔액은 부족한데 예약은 완료된 상태!  

이러한 결과를 초래 할 수 있다.

그럼 순서를 어떻게 보장할까 ?  
나는 이벤트체인을 사용하기로 했다.  
이벤트 체인이란 ?  
설명 : 작성부탁
```markdown
결제완료 -> 잔액감소완료 -> 예약완료
(PaymentCreatedEvent -> BalanceDecreasedEvent -> ReservationCompletedEvent)

실패시: 역순으로 보상 트랜잭션
예약실패 -> 잔액원복 -> 결제취소
```

그럼 마지막으로 변경된 코드
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
void 예약완료(BalanceDecreasedEvent event) {  // 잔액감소 완료 이벤트 수신 (변경!)
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
void 토큰만료(ReservationCompletedEvent event) {  // 예약완료 이벤트 수신 (추가!)
    try {
        토큰만료();
        이벤트발행(TokenExpiredEvent);  // 토큰만료 이벤트 발행
    } catch(Exception e) {
        이벤트발행(ReservationCompensationEvent);
        이벤트발행(BalanceCompensationEvent);
        이벤트발행(PaymentCompensationEvent);
    }
}

// 보상 트랜잭션 처리
@TransactionalEventListener
void 잔액원복(BalanceCompensationEvent event) {
    잔액증가();  // 20,000 -> 100,000
}

@TransactionalEventListener
void 결제취소(PaymentCompensationEvent event) {
    결제취소처리();  // 상태: PENDING -> CANCELED
}
```
이런식으로 결제생성이후 PaymentCreatedEvent 발행 -> 잔액감소 메서드가 이벤트 수신 이후 BalanceDecreasedEvent 이벤트 발행 등  
을 진행하게 되어 순서가 보장된다.


e.g. 예약 기능에서 좌석 상태도 변경하고.. 유저 금액도 차감하고.. ( 원래 한 트랜잭션 )   
얘를 트랜잭션을 분리할 수 있을까? 요런 측면을 고민해보기  
tx 1 { 결제 생성 } <-> 사람마다 다른 이유 // 예약 + 결제인 사람도 있구..  
결제생성 이벤트 -> tx 2 { 해당 좌석 상태 변경 }  
결제생성 이벤트 -> tx 3 { 유저 금액 차감 }  
애플리케이션 이벤트를 여기서 활용해보기!  
@TransactionalEventListener <- 요거 배웠자나요 ㅠㅠ  
ApplicationEventPublisher.publish()  
쪼개서 동작하면 어떤일이 벌어질까?  
[2] 문제점을 찾기
e.g. 그럼 유저 금액 차감이 실패했음. 근데 물리적으로 같은 트랜잭션이 아님.  
근데 결제생성은 이미 된 상태 ( 커밋 ) , 해당 좌석 상태 변경은 성공했음 ( 커밋 )  
유저 금액 차감 ( 롤백 ) -> 유저는 돈을 안내고 결제를 성공함.  
그럼 이걸 어떻게 해결해야할까? = 보상 트랜잭션  
[3] 보상 트랜잭션을 어떻게 적용할 수 있을까?  
e.g. 어떻게 동작하게 해야할까?  
----> 설계 과제에 필요한 문서 내용  백이 되겠죠?  
그게 싫어서 try-catch 로 해당 함수를 감아놨음..? 그럼 그 응답이 올 때까지 기다림.트랜잭션은  
근데 "메인 로직" 과 "부가 로직" 이라는 관심사 기준이 있는데
- 메인 로직 : 예약/결제
- 부가 로직 : (예약/결제가 성공하면) 데이터 전송  
  (예약/결제가 성공하면) 슬랙으로 내부 개발자한테 알림  
  (예약/결제가 성공하면) 유저한테 카카오톡 알림 전송

  	@Transactional
  	예약/결제() {
  		예약~~
  		예약~~
  		결제~~
  		try {
  			데이터플랫폼 전송() // 5초 기다렸다가 실패함
  			전송()
  		} catch {
  			
  		}
  	}
[4] API 호출하는 가짜 객체를 만들고, 걔가 실패하는 함수로 만든다.  
그리고, 걔가 실패하더라도 좌석예약/결제 은 영향을 받지 않도록  
ApplicationEvent 를 활용해서 느슨한 결합으로 개선해보기.  
e.g. 예약/결제 --- if 성공하면 --> 예약/결제 내역을 API Call 로 우리 데이터 플랫폼에 전송  
근데 만약에 데이터 플랫폼 전송이 실패하면? 롤

----> 구현 과제
------------------- 허재의 채점기준

Try If you want (설계 문서 -> 구현) 을 해오면 피드백은 드릴 것.