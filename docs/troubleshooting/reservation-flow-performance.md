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

## 최종 성능 테스트 결과

| 단계 | avg | p(95) | 임계값 | 상태 |
|---|---|---|---|---|
| 1. 토큰 발급 | 6.2ms | 9.4ms | 200ms | ✓ |
| 2. 스케줄 조회 | 5.7ms | 8.1ms | 500ms | ✓ |
| 3. 좌석 조회 | 43ms | 50.7ms | 500ms | ✓ |
| 4. 좌석 예약 | 3.4ms | 5.4ms | 1000ms | ✓ |
| 5. 결제 | 9.2ms | 12.4ms | 1000ms | ✓ |

모든 단계 임계값 통과. 응답시간 관점에서 정상.

---

## 예약/결제 실패율 분석

**예약 39% 성공 (137건 실패) — 정상**

100개 좌석에 동시 요청이 몰려 같은 좌석을 중복 예약 시도.
이미 예약된 좌석은 비관적 락 or 낙관적 락으로 실패 처리 → **올바른 동작**.
성능 문제가 아닌 비즈니스 로직의 정상 동작이다.

**결제 85% 성공 (13건 실패) — 정상**

userId가 1~10 랜덤이라 같은 유저가 동시에 결제를 시도하는 경우 발생.
잔액 차감에 분산 락이 걸려있어 동시 요청은 락 타임아웃으로 실패 → **동시성 보호가 올바르게 작동**.

**예약 상태 분포 (테스트 후)**
```
CONFIRMED      : 78   (결제 완료)
EXPIRED        : 90   (이미 예약된 좌석 시도 — 정상 실패)
PENDING_PAYMENT: 14   (동시 락 충돌로 결제 미완료)
```

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
