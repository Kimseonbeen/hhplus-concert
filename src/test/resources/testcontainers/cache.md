# Redis 기반 대기열 관리 시스템 아키텍처

---

## 📌 1. 시스템 아키텍처

### 🔹 핵심 컴포넌트

```java
public class QueueTokenService {
    private static final long MAX_ACTIVE_TOKEN_COUNT = 100;  // 최대 활성 토큰
    private final QueueTokenRepository queueTokenRepository;
}

public class QueueTokenRedisRepositoryImpl {
    private static final String WAITING_TOKEN_PREFIX = "waiting-token";
    private static final String ACTIVE_TOKEN_PREFIX = "active-token";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
}
```

### 🔹 데이터 구조

#### ✅ 활성 토큰
- **저장 방식**: Redis String
- **키 형식**: `active-token:{token}`
- **TTL**: 10분
- **최대 개수**: 100개

#### ✅ 대기 토큰
- **저장 방식**: Redis Sorted Set
- **키**: `waiting-token`
- **Score**: 시스템 시간(milliseconds)

---

## 🚀 2. 주요 기능 상세

### 🔹 토큰 발급 프로세스
```java
@Transactional
public QueueToken issueQueueToken(long userId) {
    long activeCount = queueTokenRepository.getActiveTokenCount();
    long waitingCount = queueTokenRepository.getWaitingTokenCount();
    QueueToken queueToken = QueueToken.issueToken(userId, activeCount, waitingCount);
    queueTokenRepository.save(queueToken);
    return queueToken;
}
```

### 🔹 토큰 상태 조회
```java
public Optional<QueueToken> findByToken(String token) {
    // 1. 활성 토큰 확인
    String activeToken = redisTemplate.opsForValue().get(ACTIVE_TOKEN_PREFIX + ":" + token);

    // 2. 대기 토큰 확인
    Double waitingScore = redisTemplate.opsForZSet().score(WAITING_TOKEN_PREFIX, token);
}
```

### 🔹 대기열 활성화 로직
```java
public void activateNextWaitingToken() {
    Long activeTokenCount = queueTokenRepository.getActiveTokenCount();
    if (activeTokenCount < MAX_ACTIVE_TOKEN_COUNT) {
        long needs = MAX_ACTIVE_TOKEN_COUNT - activeTokenCount;
        List<String> waitingTokens = queueTokenRepository.getWaitingTokens(needs);
        queueTokenRepository.removeWaitingTokens(waitingTokens);
        waitingTokens.forEach(queueTokenRepository::saveActiveTokens);
    }
}
```

---

## 🎯 3. 성능 최적화

### 🔹 조회 성능
- **활성 토큰 조회**: `O(1)`
- **대기열 순위 조회**: `O(log N)`
- **대기열 크기 조회**: `O(1)`

### 🔹 메모리 관리
- **TTL 기반 자동 만료**
- **결제 완료 시 즉시 삭제**
- **대기열 자동 정리**

---

## ⚡ 4. 주요 캐시 전략

### 🔹 4.1 Look-Aside
- **개념 및 동작 방식**: 데이터 요청 시 먼저 캐시를 조회하고, 없으면 DB에서 조회 후 캐시에 저장
- **장점**: 필요할 때만 캐시에 적재하여 불필요한 데이터 캐싱 방지
- **단점**: 최초 요청 시 캐시 미스(Cache Miss) 발생 가능
- **활용 사례**: 자주 변경되는 데이터

### 🔹 4.2 Write-Through
- **개념 및 동작 방식**: 데이터를 캐시와 DB에 동시에 기록
- **장점**: 데이터 일관성 유지
- **단점**: 쓰기 성능 저하 가능
- **활용 사례**: 자주 변경되지 않는 데이터 캐싱

### 🔹 4.3 Write-Around
- **개념 및 동작 방식**: 데이터를 DB에 먼저 기록하고, 필요할 때만 캐시에 적재
- **장점**: 불필요한 데이터 캐싱 방지
- **단점**: 최초 접근 시 캐시 미스(Cache Miss) 발생 가능
- **활용 사례**: 읽기보다 쓰기가 많은 시스템

### 🔹 4.4 Write-Back
- **개념 및 동작 방식**: 데이터를 캐시에 우선 저장하고, 일정 주기 후에 DB에 반영
- **장점**: 쓰기 성능 향상
- **단점**: 장애 발생 시 데이터 유실 가능
- **활용 사례**: 쓰기 작업이 많고 성능이 중요한 서비스

### 🔹 4.5 Lazy Loading (Read-Through)
- **개념 및 동작 방식**: 필요할 때 데이터를 캐시에 적재
- **장점**: 불필요한 데이터 캐싱 방지
- **단점**: 초기 접근 시 응답 속도 저하 가능
- **활용 사례**: 자주 사용되지만 변경이 적은 데이터

---

## 🎯 5. 캐시 전략 활용 방안

| 캐시 전략 | 활용 상황 |
|-----------|----------------------|
| **Look-Aside** | 캐시 적중률을 높이고 싶은 경우 |
| **Write-Through** | 데이터 일관성이 중요한 경우 |
| **Write-Around** | 쓰기 빈도가 높은 경우 |
| **Write-Back** | 쓰기 성능이 중요한 경우 |
| **Lazy Loading (Read-Through)** | 자주 조회하지만 갱신 빈도가 낮은 데이터 |

---

## ⚠️ 6. 캐시 스탬피드(Cache Stampede) 현상

### 🔹 개념
- 동일한 데이터의 캐시가 만료될 때, 다수의 요청이 동시에 DB를 조회하여 부하가 급증하는 현상

### 🔹 원인 및 발생 조건
- 캐시 만료 후 다수의 요청이 동시에 들어오는 경우
- 캐시를 초기화하는 과정에서 발생하는 동시성 문제

### 🔹 해결 방법
- **분산 락 적용**: 특정 클라이언트가 먼저 캐시를 갱신하도록 설정
- **백오프(Backoff) 전략**: 요청을 지연시키거나 랜덤 딜레이 적용
- **캐시 만료 시간 분산**: 같은 데이터라도 유사한 만료 시간을 가지지 않도록 설정
- **데이터 더블 캐싱(Double Caching)**: 이전 데이터를 유지한 채로 새로운 데이터를 준비하는 방식

---

## 📈 7. 조회 성능 개선을 위한 캐싱 및 Redis 활용

### 🔹 콘서트 스케줄 조회 최적화
- 예약 가능한 콘서트 목록 조회는 변경 빈도가 낮은 데이터로 확인
- **3분의 TTL 적용**: 데이터의 일관성을 유지하면서 캐시를 효과적으로 활용 가능
- **캐시 무효화(Eviction) 적용**: 콘서트 상태 변경 시 캐시 삭제하여 데이터 일관성 유지

```java
@Column(nullable = false)
@Enumerated(EnumType.STRING)
private ConcertScheduleStatus status;
```

---

