# 인덱스와 성능 테스트

## 인덱스란

DB가 데이터를 찾을 때 테이블 전체를 스캔(풀스캔)하지 않고 빠르게 찾을 수 있도록 돕는 자료구조.
책의 목차처럼, 원하는 데이터의 위치를 미리 정리해둔 것.

---

## 언제 인덱스를 설계해야 하나

**설계 단계에서 작성하는 것이 이상적이다.**

인덱스가 필요한 시점은 쿼리를 작성할 때 이미 보인다.

- `WHERE` 조건에 자주 쓰이는 컬럼
- `JOIN` 키로 사용되는 컬럼
- `ORDER BY`, `GROUP BY`에 쓰이는 컬럼
- 복합 조건이면 복합 인덱스 고려

### 이상적인 흐름

```
ERD 설계 → 인덱스 설계 (WHERE/JOIN 기준)
    → 개발 → 성능 테스트 → 누락된 인덱스 추가
```

---

## 페이지네이션

### 왜 필요한가

API가 조건에 맞는 데이터를 **전부 반환**하면:
- 데이터가 많을수록 응답 크기가 선형으로 증가
- 네트워크 전송 비용 증가
- 클라이언트 렌더링 부하 증가

이번 케이스에서 인덱스 없이 전체 반환 시 **1.0 GB** 데이터가 전송됐다.

### Spring Data JPA 페이지네이션

**Repository**

```java
@Query(
    value = "select id, concert_id, concert_date, status " +
            "from concert_schedule " +
            "where concert_id = :concertId " +
            "and status = 'AVAILABLE' " +
            "and concert_date >= SYSDATE() " +
            "order by concert_date asc",
    countQuery = "select count(*) from concert_schedule " +
                 "where concert_id = :concertId " +
                 "and status = 'AVAILABLE' " +
                 "and concert_date >= SYSDATE()",
    nativeQuery = true
)
Page<ConcertSchedule> findAvailableSchedule(@Param("concertId") Long concertId, Pageable pageable);
```

**Controller**

```java
@GetMapping("/{concertId}/schedules")
public ResponseEntity<Page<ConcertScheduleResponse>> getConcertSchedule(
        @PathVariable Long concertId,
        @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(concertService.getConcertSchedules(concertId, pageable));
}
```

- `@PageableDefault(size = 20)` — 클라이언트가 size를 지정하지 않으면 기본 20개
- 클라이언트는 `?page=0&size=20` 쿼리 파라미터로 페이지 제어

### 네이티브 쿼리에서 Pageable sort 주의점

`@PageableDefault(sort = "concertDate")`처럼 sort를 지정하면,
Spring이 자동으로 `ORDER BY concertDate asc`를 SQL에 붙이는데
**네이티브 쿼리는 Java 필드명이 아닌 DB 컬럼명을 사용**하므로 에러가 발생한다.

```
Unknown column 'concertDate' in 'order clause'  ← Java 필드명
```

**해결:** sort를 `@PageableDefault`에서 제거하고 쿼리에 `ORDER BY concert_date asc` 직접 작성.

### 페이지네이션 적용 효과

| 지표            | 페이지네이션 전 | 페이지네이션 후 |
| ------------- | -------- | -------- |
| data_received | 1.0 GB   | 3.5 MB   |
| 응답 크기         | 전체 반환    | 20개 제한   |

---

## 문제 발견 과정 (concert_schedule)

### 쿼리

```sql
SELECT id, concert_id, concert_date, status
FROM concert_schedule
WHERE concert_id = :concertId
  AND status = 'AVAILABLE'
  AND concert_date >= SYSDATE()
ORDER BY concert_date ASC
```

### 초기 상태

- `concert_schedule` 테이블: **100,003개 행**
- 인덱스: **PK(id)만 존재**
- 결과: `WHERE` 조건 3개 전부 **풀스캔**

### 성능 테스트 결과 (인덱스 없음)

| 지표 | 값 |
|---|---|
| 평균 응답시간 | 358ms |
| p(95) | 994ms |
| max | 1.42s |
| 임계값(p95 < 500ms) | ✗ 실패 |

---

## 인덱스 추가

```sql
CREATE INDEX idx_concert_schedule_search
ON concert_schedule (concert_id, status, concert_date);
```

### 복합 인덱스 컬럼 순서 선정 이유

| 순서 | 컬럼 | 이유 |
|---|---|---|
| 1 | `concert_id` | 동등 조건(`=`), 카디널리티 높음 |
| 2 | `status` | 동등 조건(`=`), concert_id 이후 필터링 |
| 3 | `concert_date` | 범위 조건(`>=`), 마지막에 위치해야 인덱스 범위 스캔 가능 |

> 범위 조건(`>`, `<`, `>=`, `BETWEEN`) 컬럼은 복합 인덱스에서 마지막에 위치해야 한다.
> 범위 조건 이후 컬럼은 인덱스를 활용하지 못한다.

### 실행계획 확인

```sql
EXPLAIN SELECT ...
```

```
type: ref
key:  idx_concert_schedule_search
Extra: Using where; Using index
```

- `Using index` — 테이블 접근 없이 인덱스만으로 처리 완료 (커버링 인덱스)

---

## 성능 테스트 결과 비교

| 지표 | 인덱스 전 | 인덱스 후 |
|---|---|---|
| 평균 응답시간 | 358ms | **10.89ms** |
| p(95) | 994ms | **15.89ms** |
| max | 1.42s | **36ms** |
| 성공률 | 100% | 100% |
| 임계값(p95 < 500ms) | ✗ 실패 | ✓ 통과 |

**약 33배 개선**

