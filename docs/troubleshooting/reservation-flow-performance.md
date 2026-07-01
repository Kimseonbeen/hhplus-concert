# 예약 플로우 성능 테스트

## 개요

단일 API 성능 테스트는 실제 서비스 흐름을 반영하지 못한다.
예약 서비스는 토큰 발급 → 스케줄 조회 → 좌석 조회 → 예약 → 결제가 하나의 연속된 흐름이므로,
전체 플로우를 하나의 시나리오로 구성해 **단계별 병목을 탐지**한다.

**개별 API 테스트 vs 플로우 테스트**

| | 개별 API 테스트 | 플로우 테스트 |
|---|---|---|
| 현실 반영도 | 낮음 | 높음 |
| 병목 위치 파악 | 불가 | 단계별 확인 가능 |
| 인프라 문제 발견 | 어려움 | 전체 부하로 드러남 |

---

## 테스트 시나리오

```
토큰 발급 → 스케줄 조회 → 좌석 조회 → 좌석 예약 → 결제
```

### k6 시나리오 설정

```javascript
scenarios: {
  reservation_flow: {
    executor: 'ramping-arrival-rate',  // VU 수가 아닌 RPS 기준으로 부하 제어
    startRate: 10,
    timeUnit: '1s',
    preAllocatedVUs: 50,
    maxVUs: 200,
    stages: [
      { duration: '10s', target: 10 },  // 워밍업
      { duration: '20s', target: 30 },  // 부하 증가
      { duration: '20s', target: 50 },  // 피크 부하
      { duration: '10s', target: 10 },  // 부하 감소
    ],
  }
}
```

### 단계별 병목 탐지 — Trend 커스텀 메트릭

k6의 `Trend` 메트릭으로 각 단계 응답시간을 Grafana에서 개별 확인한다.

```javascript
import { Trend } from "k6/metrics";

const tokenTrend    = new Trend("duration_01_token");
const scheduleTrend = new Trend("duration_02_schedule");
const seatTrend     = new Trend("duration_03_seat");
const reserveTrend  = new Trend("duration_04_reserve");
const paymentTrend  = new Trend("duration_05_payment");

export default function () {
  const tokenRes = http.post(...);
  tokenTrend.add(tokenRes.timings.duration);  // 단계별 시간 기록
  // ...
}
```

---

## 임계값 설정 기준

각 단계의 특성에 따라 임계값을 다르게 설정한다.
무조건 빠른 기준보다 **API의 역할과 기술 스택**에 맞는 현실적인 기준이 중요하다.

```javascript
thresholds: {
  'duration_01_token':    ['p(95)<200'],   // Redis — 메모리 기반, 매우 빠름
  'duration_02_schedule': ['p(95)<500'],   // DB 조회 + 인덱스
  'duration_03_seat':     ['p(95)<500'],   // DB 조회 + 인덱스
  'duration_04_reserve':  ['p(95)<1000'],  // DB 트랜잭션 + 분산 락
  'duration_05_payment':  ['p(95)<1000'],  // DB 트랜잭션 + 분산 락 + 아웃박스
}
```

| 단계 | 임계값 | 설정 이유 |
|---|---|---|
| 토큰 발급 | p(95) < 200ms | Redis 기반 메모리 작업. 네트워크 레이턴시 정도만 허용. 티켓팅 오픈 시 가장 많은 요청이 몰리므로 엄격하게 설정 |
| 스케줄 조회 | p(95) < 500ms | DB 읽기 작업. 인덱스로 최적화. 여러 사용자가 동시 조회하므로 일반적인 웹 API 기준 적용 |
| 좌석 조회 | p(95) < 500ms | 스케줄 조회와 동일한 성격의 읽기 작업 |
| 좌석 예약 | p(95) < 1000ms | 비관적 락(또는 분산 락) + DB 트랜잭션 포함. 락 대기 시간이 추가되므로 여유롭게 설정 |
| 결제 | p(95) < 1000ms | 잔액 차감 분산 락 + 결제 기록 + 아웃박스 이벤트 저장까지 여러 DB 작업 포함 |

> **p(95)를 기준으로 하는 이유**
> avg(평균)은 극단값에 민감하고, max는 일시적 이상치에 영향받는다.
> p(95)는 "전체 요청의 95%가 이 시간 안에 응답했다"는 의미로
> 대부분의 사용자 경험을 대표하는 가장 일반적인 기준이다.

---

## 발견된 문제들과 해결 과정

### 문제 1. DB 커넥션 풀 부족

**증상**
- 토큰 발급 p(95) = 10초 (타임아웃)
- `WARN: Insufficient VUs, reached 200 active VUs` 경고 발생
- 거의 모든 요청이 타임아웃으로 실패

**원인**
`application.yml`에 커넥션 풀이 3개로 설정되어 있었다.
```yaml
hikari:
  maximum-pool-size: 3  # ← 로컬 개발용으로 작게 설정된 값
```
50 TPS 부하에서 3개 커넥션이 모두 점유되면 나머지 요청은 풀을 기다리다 타임아웃.

VU가 타임아웃으로 오래 점유되면 새 요청을 처리할 VU가 부족해지고,
결국 200개 VU 한계에 도달해 요청 자체가 드랍된다.

**해결**
```yaml
hikari:
  maximum-pool-size: 20
```

> **커넥션 풀 적정 크기 공식**
> `(CPU 코어 수 × 2) + 디스크 수`
> MacBook 8코어 기준 약 17~20개가 적정하다.
> 무작정 늘리면 MySQL `max_connections` 초과 및 커넥션당 메모리 낭비가 발생한다.

**결과**
커넥션 풀 증가 후 타임아웃 사라지고 모든 단계 응답시간 정상화.

---

### 문제 2. expired_at 컬럼 타입 오류 (DATE vs DATETIME)

**증상**
- 결제 요청 시 `"예약이 만료되었습니다."` 에러
- 결제 성공률 0%
- 예약 직후 바로 결제해도 만료 처리

**원인**
DB `reservation.expired_at` 컬럼이 `DATE` 타입으로 생성되어 있었다.
```sql
DESCRIBE reservation;
-- expired_at | date  ← DATETIME이어야 함
```

Java에서 `LocalDateTime.now().plusMinutes(5)`로 생성한 만료 시간이
`DATE` 타입은 시간 정보를 저장하지 못해 날짜만 저장된다.

```
Java 생성값:  2026-05-06T16:01:26  (5분 뒤)
DB 저장값:    2026-05-06 00:00:00  (자정으로 잘림)
현재 시각:    2026-05-06 16:01:28
→ isExpired() : 00:00:00 < 16:01:28 → 이미 만료!
```

**해결**
```sql
ALTER TABLE reservation MODIFY COLUMN expired_at DATETIME;
```

**교훈**
엔티티의 Java 타입(`LocalDateTime`)과 DB 컬럼 타입(`DATETIME`)이 일치해야 한다.
단위 테스트나 소량 데이터에서는 드러나지 않고, 실제 흐름 테스트에서 발견되는 유형의 문제다.

---

### 문제 3. Hibernate NORMALIZE_UTC + LocalDateTime 타임존 불일치

**증상**
`DATE` → `DATETIME` 변경 후에도 결제 실패 지속.
DB 조회 시 `expired_at = 07:01:27`이고 `NOW() = 16:01:28`이라 여전히 만료.

**원인**
`application.yml`에 다음 설정이 있었다:
```yaml
spring:
  jpa:
    properties:
      hibernate.timezone.default_storage: NORMALIZE_UTC
      hibernate.jdbc.time_zone: UTC
```

`NORMALIZE_UTC`는 `LocalDateTime`을 저장할 때 UTC로 정규화한다.
서버가 KST(UTC+9) 기준이면 다음과 같이 동작한다:

```
Java 생성:   LocalDateTime.now() = 2026-05-06 16:01:26 (KST)
NORMALIZE_UTC 적용 → UTC 변환: 2026-05-06 07:01:26
DB 저장값:   2026-05-06 07:01:26

읽어올 때:   LocalDateTime으로 07:01:26 반환 (타임존 정보 없음)
isExpired(): 07:01:26 < LocalDateTime.now() (16:01:xx) → 만료!
```

`LocalDateTime`은 타임존 정보가 없는 순수 날짜/시간값이다.
저장할 때 UTC로 변환했지만 읽어올 때는 그 사실을 모르고 로컬 시간과 비교하므로 항상 만료 처리된다.

**해결**
타임존 변환 설정 제거. `LocalDateTime`은 변환 없이 있는 그대로 저장.
```yaml
# 제거
# hibernate.timezone.default_storage: NORMALIZE_UTC
# hibernate.jdbc.time_zone: UTC
```

> **근본적인 해결책**
> 타임존을 정확히 다루려면 `LocalDateTime` 대신 `Instant` 또는 `ZonedDateTime`을 사용해야 한다.
> - `Instant`: 타임존 없이 UTC 기준 절대 시각 저장
> - `ZonedDateTime`: 타임존 정보 포함해서 저장
>
> `LocalDateTime` + `NORMALIZE_UTC` 조합은 저장/조회 시 타임존 불일치가 발생하므로 사용하지 않는 것이 좋다.

---

## TPS 스케일업 테스트

### 배경

초기 TPS 50 검증 이후, 실제 콘서트 티켓팅 수준의 트래픽을 검증하기 위해 TPS를 단계적으로 올렸다.

---

### 발견된 문제들과 해결 과정

#### 문제 1. DB 데이터 — concert_date 과거 날짜

**증상**
TPS 500으로 올리자 좌석 예약 성공률 1% (3/152).

**원인**
`concert_schedule.concert_date`가 과거 날짜로 생성된 데이터가 90,001건 존재.
`ConcertSchedule.checkIsAvailable()`에서 `concertDate.isBefore(LocalDateTime.now())` 검증을 통과하지 못해 `CONCERT_DATE_EXPIRED` 예외 발생.

> 앱은 Docker MySQL이 아닌 **Homebrew MySQL(localhost:3306)**에 연결된다.
> 테스트 전 반드시 아래 쿼리로 확인한다.

```sql
-- 과거 날짜 스케줄 수 확인
SELECT COUNT(*) FROM concert_schedule WHERE concert_date < CURDATE();

-- 일괄 수정
UPDATE concert_schedule SET concert_date = '2026-08-01' WHERE concert_date < CURDATE();
```

**결과**
수정 후 예약 성공률 1% → 43%로 회복.

---

#### 문제 2. userId 범위 협소로 결제 분산 락 충돌 집중

**증상**
TPS 500 기준 결제 성공률 55%.

**원인**
k6 스크립트에서 `userId = Math.floor(Math.random() * 10) + 1`로 10명만 사용.
500 TPS에서 동일 userId가 동시에 결제 시도 → 잔액 차감 분산 락 충돌 집중.

**해결**
DB에 유저 200명 + 잔액(10,000,000원) 추가, k6 userId 범위를 1~200으로 확대.

```sql
INSERT INTO users (name, version)
SELECT CONCAT('user', n), 0 FROM (WITH RECURSIVE seq(n) AS (
  SELECT 12 UNION ALL SELECT n+1 FROM seq WHERE n < 200
) SELECT n FROM seq) t;

INSERT INTO balance (user_id, amount)
SELECT id, 10000000 FROM users WHERE id >= 11;
```

**결과**
결제 성공률 55% → 100%.

---

#### 문제 3. 좌석 조회 — 페이지네이션 없이 전체 반환

**증상**
`duration_03_seat` avg 33ms. 다른 단계(1~2ms)보다 10배 이상 느림.

**원인**
`SeatRepository`가 `scheduleId` 기준 전체 좌석(30,000개)을 한 번에 반환.
페이지네이션 없이 풀스캔 + 대량 직렬화 발생.

**해결**
`SeatRepository`에 `Pageable` 파라미터 추가 (기본 50개).

```java
@Query(value = "select seat_num from seat where concert_schedule_id = :concertScheduleId and status = :status",
       countQuery = "select count(*) from seat where concert_schedule_id = :concertScheduleId and status = :status",
       nativeQuery = true)
Page<Integer> findByConcertScheduleIdAndStatus(
    @Param("concertScheduleId") Long concertScheduleId,
    @Param("status") SeatStatus status,
    Pageable pageable
);
```

**결과**
좌석 조회 avg 33ms → 11ms (TPS 2,000 기준).

---

#### 문제 4. MAX_ACTIVE_USERS 하드코딩

**증상**
`QueueConstants.MAX_ACTIVE_USERS = 150L`이 코드에 고정되어 있어
서버 스펙 변경 시 코드 배포 없이 조정 불가.

**해결**
`application.yml`로 외부화.

```yaml
queue:
  token:
    max-active-users: 150
```

```java
@Value("${queue.token.max-active-users}")
private long maxActiveUsers;
```

---

### 램프업 vs 스파이크 테스트

초기 테스트는 단계적으로 TPS를 올리는 **램프업(Ramping)** 방식이었다.
이 방식은 JVM JIT 컴파일, 커넥션 풀 웜업, 캐시 안정화 시간을 주기 때문에
실제보다 좋은 결과가 나온다.

콘서트 티켓팅은 오픈 순간 모든 사용자가 동시 접속하는 **즉시 스파이크** 패턴이므로
테스트를 아래와 같이 변경했다.

```javascript
// 변경 전 — 램프업
stages: [
  { duration: '10s', target: 200  },
  { duration: '20s', target: 2000 },
  { duration: '20s', target: 5000 },
  { duration: '10s', target: 200  },
]

// 변경 후 — 스파이크
stages: [
  { duration: '5s',  target: 5000 }, // 즉시 스파이크
  { duration: '50s', target: 5000 }, // 지속 부하
  { duration: '5s',  target: 0    },
]
```

---

### TPS 단계별 결과 비교

| TPS | 테스트 방식 | 좌석 조회 p(95) | 결제 성공률 | 토큰 p(95) | 임계값 통과 |
|-----|-----------|--------------|------------|-----------|-----------|
| 500 | 램프업 | 37ms | 55% | - | ✓ |
| 1,000 | 램프업 | 26ms | 100% | 2.6ms | ✓ |
| 2,000 | 램프업 | 19ms | 100% | 2.8ms | ✓ |
| 3,000 | 램프업 | 21ms | 100% | 2.7ms | ✓ |
| 5,000 | 램프업 | 16ms | 100% | 28ms | ✓ |
| 5,000 | 스파이크 | 577ms | 100% | 218ms | ✗ (토큰·좌석 초과) |
| **4,000** | **스파이크** | **380ms** | **100%** | **91ms** | **✓** |
| 10,000 | 스파이크 | - | - | 5,762ms | ✗ (토큰 초과) |

---

### 대기열 설계와 TPS의 관계

```
TPS 4,000 유입
      ↓
  토큰 발급 (Redis)
      ↓
  ┌─ WAITING → 즉시 return (DB 부하 없음)
  └─ ACTIVE 150명 → 풀 플로우 (DB 부하 고정)
```

TPS가 높아져도 DB에 가는 실제 부하는 `max-active-users`에 의해 고정된다.
대기열이 없었다면 TPS 4,000 전체가 DB까지 도달해 시스템이 즉시 다운됐을 것이다.

**TPS 10,000에서 실패한 원인**은 DB 과부하가 아니라 Tomcat 스레드 풀(기본 200개)이
10,000개의 HTTP 연결을 동시에 처리하지 못한 것이다.
DB 보호는 대기열이 올바르게 수행하고 있었다.

---

### 최종 검증 환경 기준값

| 항목 | 값 | 근거 |
|-----|---|-----|
| 목표 TPS | 4,000 (스파이크) | 5,000 TPS 스파이크 재검증 시 토큰 p(95) 218ms·좌석 p(95) 577ms 임계값 초과 → 4,000으로 하향, 전 단계 통과 |
| max-active-users | 150 | Hikari pool 20개 기준 커넥션당 7.5명 균형 |
| Hikari pool-size | 20 | CPU 8코어 기준 적정값 (코어×2+1) |
| 좌석 조회 페이지 크기 | 50 | 응답 크기와 DB 부하의 균형점 |

> max-active-users와 Hikari pool-size는 함께 조정해야 한다.
> 서버 스펙이 올라가 pool-size를 늘릴 경우 max-active-users도 비례해서 올린다.

---

## 테스트 실행 방법

Redis active 슬롯이 가득 차면 신규 토큰이 WAITING 상태가 되어 플로우 진행이 불가하다.
매 테스트 전 Redis를 초기화하는 스크립트를 사용한다.

```bash
# k6/run-test.sh
#!/bin/bash
SCRIPT=${1:-reservation-flow-test.js}

echo "Redis 토큰 초기화 중..."
docker exec hhplus-concert-redis-1 redis-cli KEYS "active-token:*" | \
  xargs -r docker exec -i hhplus-concert-redis-1 redis-cli DEL
docker exec hhplus-concert-redis-1 redis-cli DEL "waiting-token"

/opt/homebrew/bin/k6 run --out influxdb=http://localhost:8086/k6 "k6/$SCRIPT"
```

```bash
./k6/run-test.sh                          # 기본 (전체 플로우)
./k6/run-test.sh queue-token-create-test.js   # 토큰 발급 스파이크
./k6/run-test.sh concert-schedule-test.js     # 스케줄 조회 부하
```
