STEP 15


index
1. 인덱스 설명

2. B+Tree  
   📌 1. 범위 검색에 유리  
   리프 노드가 연결 리스트 형태로 정렬되어 있어, 순차 검색이 빠름  
   예를 들어, WHERE age BETWEEN 20 AND 30 같은 범위 검색 시 한 번 찾고 나서 연결된 노드를 따라가기만 하면 됨  
   📌 2. 디스크 I/O 감소  
   내부 노드는 데이터가 아니라 인덱스 정보만 저장하므로, 한 페이지(디스크 블록)에 더 많은 인덱스 노드를 저장 가능  
   이는 트리의 높이를 낮추고, 디스크 접근 횟수를 줄이는 효과  
   📌 3. 균형 유지로 검색 성능 보장  
   트리의 높이가 일정하기 때문에 최악의 경우에도 O(log N)의 탐색 성능 보장
   트리의 일부만 불균형해지는 문제(B-Tree에서는 가능)가 없음

- 인덱스 선두 컬럼 부터 사용 이유
- B+Tree는 정렬된 **연결 리스트(리프 노드)**를 통해 탐색하지만, 탐색 시작 지점을 찾을 때는 여전히 트리 탐색을 해야 함.
  트리 탐색 시 루트 → 내부 노드 → 리프 노드 순으로 내려가기 때문에 선두 컬럼이 필수입니다.

예를 들어, (age, city, name) 복합 인덱스가 있을 때,
```markdown
SELECT * FROM users WHERE city = 'Seoul' AND name = 'John';
```
이런 쿼리를 실행하면,

age 없이 city를 검색하려고 하면 루트부터 탐색할 방법이 없음 → 인덱스 미사용 가능성 증가  
결국 리프 노드를 모두 스캔해야 해서 성능 저하  
즉, 선두 컬럼 없이 후속 컬럼부터 검색하면 B+Tree의 장점을 제대로 활용할 수 없음  


나의 시나리오에서 수행되는 쿼리들을 수집해보고,   
필요하다고 판단되는 인덱스를 추가하고 쿼리의 성능개선 정도를 작성

1. 자주 조회하는 쿼리, 복잡한 쿼리 파악

### 콘서트 예약가능 좌석 조회

- concert_id: 최대 30개
- schedule당 좌석: 50개
- 총 데이터: 최대 1,500건
- 조회 패턴: concert_schedule_id로 최대 50건만 조회
```markdown
SELECT seat_num
FROM seat
WHERE concert_schedule_id = 1
AND status = 'AVAILABLE';
```
- 미래에 대형 콘서트를 진행한다는 데이터가 증가할 가능성이 있어 인덱스를 추가
- 지금은 인덱스를 적용해도 성능 개선 차이가 크지 않아
- concert_id : 30개
- schedule당 좌석 : 30,000개
- 총 데이터 : 900,000건  
가정 하여 성능 테스트 진행함

1. 작업범위 결정조건 관점
```markdown
CREATE INDEX idx_seat_status ON seat (concert_schedule_id, status);

WHERE concert_schedule_id = 1    -- 첫 번째 작업범위 결정
-- 900,000 → 30,000 (3.33%)
AND status = 'AVAILABLE'       -- 두 번째 작업범위 결정
-- 30,000 → 18,000 (60%)

```
> concert_schedule_id (첫 번째 컬럼)  
   동등 조건(=)으로 사용  
   전체의 6.67%로 대폭 축소  
   높은 선택도(30개 값으로 명확히 구분)  
   매우 효과적인 작업범위 결정 조건  
   status (두 번째 컬럼)  
   동등 조건(=)으로 사용
   30,000건에서 18,000건으로 축소  
   낮은 선택도(2개 값만 존재)  
   두 번째 작업범위 결정 조건으로 적합



### 컬럼 순서 선정 이유:

1. 첫 번쨰 컬럼 concert_shedule_id 선정
- 최대 30개의 유니크 값
- 전체 데이터를 최대 3.33%로 줄임
- 동등 비교 조건(=)

2. 두 번째 컬럼 status 선정
- 동등 비교 조건 (=)
- 하지만 값이 2개뿐 (AVAILABLE/RESERVED)
- 카디널리티가 낮음 (60:40 비율)

3. 순서가 중요한 이유
(status, concert_schedule_id)로 했다면:

status로 먼저 검색 (전체의 60%)  
그 다음 concert_schedule_id로 필터링  
첫 단계에서 충분한 필터링이 안됨  

(concert_schedule_id, status)가 더 효율적:  

더 높은 카디널리티를 가진 concert_schedule_id로 먼저 크게 줄임  
status로 추가 필터링  
더 효율적인 데이터 접근 가능  


4. 인덱스 적용 이후 explain
```markdown
CREATE INDEX idx_concert_schedule ON concert_schedule (concert_id);
```

1, 인덱스 적용 전

| id | select_type | table | partitions | type | possible_keys | key | key_len | ref | rows    | filtered | Extra |
|---|-------------|-------|------------|------|---------------|-----|----------|-----|---------|----------|--------|
| 1 | SIMPLE | seat  | NULL | ALL | NULL | NULL | NULL | NULL | 897,227 | 1        | Using where |

이 실행 계획의 주요 의미:
1. type: ALL (전체 테이블 스캔)
2. key: NULL (사용된 인덱스 없음)
3. rows: 897,227 (읽을 것으로 예상되는 행 수)
4. filtered: 1 (조건을 만족할 것으로 예상되는 행의 비율)
5. Extra: Using where (조건으로 필터링 필요)

앞선 실행계획과 비교하면 인덱스를 사용하지 않아 더 많은 행을 스캔하게 됩니다.
2. 인덱스 전용 후 

1, explain

| id | select_type | table | partitions | type | possible_keys | key | key_len | ref | rows   | filtered | Extra |
|---|-------------|--------|------------|------|---------------|-----|----------|-----|--------|----------|-------|
| 1 | SIMPLE | seat | NULL | ref | idx_seat_status | idx_seat_status | 132 | const | 35,776 | 100      | NULL  |

이 실행 계획의 주요 의미:
1. type: ref (인덱스를 사용한 동등 조건 검색)
2. key : idx_seat_status
3. rows: 3939 (읽을 것으로 예상되는 행 수)
4. filtered: 100 (조건을 만족할 것으로 예상되는 행의 비율)
5. Extra: NULL (복합 인덱스로 모든 조건 처리, 추가 작업 없음)


5. 인덱스 적용 이후 EXPLAIN ANALYZE
이전
```markdown
-> Table scan on seat
cost=90510            -- 예상 비용 (인덱스보다 15배 높음)
rows=897808           -- 전체 테이블 예상 행 수
actual time=0.361..136 -- 테이블 스캔 시간
- 0.361: 첫 행 스캔
- 136: 전체 스캔 완료

-> Filter 단계
actual time=0.414..200 -- 필터링 포함 총 실행 시간
- 0.414: 첫 행 필터링
- 200: 모든 필터링 완료
  rows=18112            -- 최종 조회된 행 수

```

이후 
```markdown
-> Index lookup on seat using idx_seat_status
(concert_schedule_id = 15, status = 'AVAILABLE')
cost=5765              -- 예상 비용
rows=35776            -- 예상 행 수 (실제보다 높게 예측)
actual time=7.35..50.7 -- 실제 실행 시간
- 7.35: 첫 행 조회
- 50.7: 전체 실행 완료
  rows=18112            -- 실제 조회된 행 수
```

성능 비교 
- 인덱스 사용 : 50.7ms
- 테이블 스캔 : 200ms
- 인덱스가 약 4배 빠름