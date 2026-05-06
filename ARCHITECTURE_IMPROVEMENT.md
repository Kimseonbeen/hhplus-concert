# 아키텍처 개선 방향

## 1. `ReservationFacade.completePayment()` 책임 분리
**현재 문제**: 하나의 `@Transactional` 메서드에서 5개 서비스를 호출하며, DB 작업과 Redis 작업(토큰 삭제)이 혼재
- Redis `delete()`는 DB 트랜잭션 롤백 시 함께 롤백되지 않음 → 결제 실패해도 토큰이 삭제될 수 있음
- 서비스 5개에 직접 의존 → 높은 결합도

**개선 방향**:
- 토큰 삭제를 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 이동 → DB 커밋 성공 후에만 토큰 삭제
- Facade의 역할을 "오케스트레이션"으로 한정하고, 각 도메인 서비스의 책임 명확화
- 이벤트 기반으로 전환하여 서비스 간 직접 의존 제거

## 2. 트랜잭셔널 아웃박스 패턴 완성
**현재 문제**: `saveOutbox()` 메서드가 비어 있어 outbox 패턴이 실질적으로 미작동
- 외부 시스템(데이터 플랫폼) 호출 실패 시 재시도 메커니즘 없음

**개선 방향**:
- Outbox 테이블 설계 및 `saveOutbox()` 구현
- 스케줄러 또는 CDC(Change Data Capture)로 미전송 이벤트 재발행
- 멱등성 키를 활용한 중복 처리 방지

## 3. 결제 플로우 통일 (단일 트랜잭션 → 이벤트 기반 전환)
**현재 문제**: 단일 트랜잭션 방식(운영)과 Kafka 사가 방식(개발 중)이 공존하여 코드 혼란
- 같은 비즈니스 로직이 두 곳에 분산
- 마이그레이션 경로가 불분명

**개선 방향**:
- Phase 1: 현재 단일 트랜잭션 방식을 안정화 (outbox 패턴 완성)
- Phase 2: Kafka 사가를 별도 브랜치에서 완성 후 통합 테스트
- Phase 3: Feature Flag 또는 점진적 전환으로 운영 플로우 교체
- `/test` 엔드포인트의 사가 관련 코드를 운영 코드와 명확히 분리

## 4. 모듈 간 결합도 감소
**현재 문제**: 일부 서비스가 presentation 계층 DTO에 직접 의존
- `QueueTokenService`가 `QueueTokenRequest`(presentation DTO)를 파라미터로 받음
- `ConcertService`가 presentation DTO에 의존

**개선 방향**:
- 서비스 계층은 도메인 객체 또는 Command/Query 객체만 받도록 통일
- presentation → application/domain 방향의 단방향 의존만 허용
- 각 모듈의 계층 간 의존 방향을 일관성 있게 유지

## 5. 이벤트 리스너 정리
**현재 문제**: `PaymentCompletedEvent`를 두 개의 리스너가 중복 구독
- `PaymentEventListener`와 `ReservationEventListener`가 동일 이벤트 처리
- 데이터 플랫폼 API가 2번 호출됨

**개선 방향**:
- 이벤트-리스너 매핑을 1:1로 정리하거나, 명확한 책임 분리
- 이벤트 흐름도를 문서화하여 중복 구독 방지
- 이벤트 핸들러에 멱등성 보장 로직 추가

## 6. Redis 대기열 성능 개선
**현재 문제**: `getActiveTokenCount()`가 `keys("active-token:*")` 패턴 매칭 사용
- Redis `KEYS` 명령은 O(N) — 토큰 수 증가 시 성능 저하
- 운영 환경에서 Redis 블로킹 위험

**개선 방향**:
- `KEYS` 대신 별도 카운터(INCR/DECR) 또는 `SCAN` 사용
- 활성 토큰 수를 별도 키로 관리하여 O(1) 조회
- 토큰 생성/삭제 시 카운터 동기화

## 7. 테스트 코드 구조 개선
**현재 문제**: `PaymentConsumer_test.java`가 `src/main`에 위치하여 운영 빌드에 포함

**개선 방향**:
- 테스트 코드를 `src/test`로 이동
- 통합 테스트와 단위 테스트 분리
- Kafka 관련 테스트는 `@EmbeddedKafka` 또는 Testcontainers 활용