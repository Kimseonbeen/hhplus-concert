# 콘서트 예약 시스템 인덱스 최적화

## 1. 인덱스 개요
인덱스는 데이터베이스에서 검색 성능을 향상시키기 위한 자료구조입니다. B+Tree 인덱스를 활용하면 검색 속도를 O(log N)으로 유지할 수 있으며, 범위 검색 및 디스크 I/O 최적화가 가능합니다.

## 2. B+Tree의 장점
1. **범위 검색에 유리**
    - 리프 노드가 연결 리스트 형태로 정렬되어 있어 순차 검색이 빠름
    - 예: `WHERE age BETWEEN 20 AND 30`
2. **디스크 I/O 감소**
    - 내부 노드에 인덱스 정보만 저장하여 한 페이지(디스크 블록)에 더 많은 노드를 저장 가능
    - 트리 높이가 낮아져 디스크 접근 횟수 감소
3. **균형 유지로 검색 성능 보장**
    - 트리의 높이가 일정하여 최악의 경우에도 O(log N)의 탐색 성능 보장
    - 일부 불균형 문제가 발생하는 B-Tree와 차별화됨

## 3. 인덱스 선두 컬럼 사용 이유
B+Tree는 정렬된 연결 리스트(리프 노드)를 통해 탐색하지만, 탐색 시작 지점을 찾을 때 트리 탐색이 필요합니다. 따라서 **선두 컬럼을 반드시 사용해야 합니다.**

예를 들어, `(age, city, name)` 복합 인덱스가 있을 때:
```sql
SELECT * FROM users WHERE city = 'Seoul' AND name = 'John';
```
이런 쿼리는 `age`가 없으므로 트리 탐색이 불가능하여 인덱스를 활용하지 못하고 전체 테이블 스캔이 발생할 수 있습니다.

---

## 4. 콘서트 예약 시스템 인덱스 최적화
### 4.1. 조회 패턴 분석
- `concert_id`: 최대 30개
- `schedule당 좌석`: 50개
- `총 데이터`: 최대 1,500건
- **조회 패턴**: `concert_schedule_id` 기준 최대 50건 조회

```sql
SELECT seat_num
FROM seat
WHERE concert_schedule_id = 1
AND status = 'AVAILABLE';
```

### 4.2. 인덱스 적용 필요성
현재 데이터가 적어 인덱스 성능 개선 효과는 미미하지만, **미래에 데이터 증가 가능성이 존재**하여 미리 최적화 필요성이 있습니다.

가정:
- `concert_id`: 30개
- `schedule당 좌석`: 30,000개
- `총 데이터`: 900,000건

### 4.3. 인덱스 생성
```sql
CREATE INDEX idx_seat_status ON seat (concert_schedule_id, status);
```

### 4.4. 작업범위 결정
```sql
WHERE concert_schedule_id = 1    -- 900,000 → 30,000 (3.33%)
AND status = 'AVAILABLE'         -- 30,000 → 18,000 (60%)
```

| 컬럼 | 조건 | 데이터 감소율 |
|-------|------|--------------|
| concert_schedule_id | `=` | 전체의 3.33%로 축소 |
| status | `=` | 30,000 → 18,000 (60%) |

#### 컬럼 순서 선정 이유
1. **첫 번째 컬럼: `concert_schedule_id`**
    - 유니크 값: 최대 30개
    - 전체 데이터를 3.33%로 축소
    - 동등 비교 조건(`=`) 사용
2. **두 번째 컬럼: `status`**
    - 동등 비교 조건(`=`) 사용
    - 값이 2개뿐 (`AVAILABLE` / `RESERVED`)
    - 카디널리티가 낮음 (60:40 비율)

**순서 변경 시 성능 문제:**
- `(status, concert_schedule_id)`로 설정하면 `status`(60%) 먼저 필터링 후 `concert_schedule_id` 적용 → 필터링 효과가 적음
- `(concert_schedule_id, status)`가 더 효율적 → **선택도가 높은 컬럼을 먼저 사용**

---

## 5. 인덱스 성능 테스트
### 5.1. 인덱스 적용 전 `EXPLAIN`

| id | select_type | table | type | key  | rows  | filtered | Extra       |
|----|------------|-------|------|------|-------|----------|-------------|
| 1  | SIMPLE     | seat  | ALL  | NULL | 897,227 | 1%      | Using where |

- **type: ALL** → 전체 테이블 스캔 발생
- **rows: 897,227** → 예상 조회 건수
- **filtered: 1%** → 최종 필터링될 비율

### 5.2. 인덱스 적용 후 `EXPLAIN`

| id | select_type | table | type | key             | rows   | filtered | Extra |
|----|------------|-------|------|----------------|--------|----------|-------|
| 1  | SIMPLE     | seat  | ref  | idx_seat_status | 35,776 | 100%     | NULL  |

- **type: ref** → 인덱스를 활용한 검색
- **rows: 35,776** → 조회 행 수 감소
- **filtered: 100%** → 불필요한 추가 필터링 없음

### 5.3. `EXPLAIN ANALYZE` 성능 비교
#### 인덱스 적용 전
```plaintext
-> Table scan on seat
cost=90510, rows=897808, actual time=0.361..136ms
-> Filter 단계
actual time=0.414..200ms, rows=18112
```
#### 인덱스 적용 후
```plaintext
-> Index lookup on seat using idx_seat_status
cost=5765, rows=35776, actual time=7.35..50.7ms
```

### 5.4. 성능 개선 효과
| 실행 방식 | 실행 시간 |
|-----------|----------|
| 테이블 스캔 | 200ms |
| 인덱스 검색 | 50.7ms |
| **속도 향상** | **약 4배 빠름** |

---

## 6. 결론
- 현재 concert_schedule_id당 30,000석 총 30개의 공연으로 구성된 데이터(900,000건)에서 인덱스 효과가 확인됨
- 인덱스 사용 시 50.7ms, 테이블 스캔 시 200ms로 약 4배 성능 차이를 보여줌
- `concert_schedule_id`와 `status`의 선택도를 고려하여 적절한 인덱스 순서 적용.
- 향후 고려사항 
  - concert_schedule_id가 증가할수록 데이터 분산도가 높아져 인덱스 효과가 더욱 증가할것으로 예상됨

