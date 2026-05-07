#!/bin/bash

SCRIPT=${1:-reservation-flow-test.js}

# ── Redis 초기화 ───────────────────────────────────────────────────────────
# active 토큰이 150개(MAX_ACTIVE_USERS) 가득 차면 신규 토큰이 WAITING 상태가 되어
# 플로우 진행 불가 → 매 테스트 전 초기화 필요
echo "Redis 토큰 초기화 중..."
docker exec hhplus-concert-redis-1 redis-cli KEYS "active-token:*" | xargs -r docker exec -i hhplus-concert-redis-1 redis-cli DEL
docker exec hhplus-concert-redis-1 redis-cli DEL "waiting-token"
echo "Redis 초기화 완료"

# ── DB 초기화 ──────────────────────────────────────────────────────────────
# 반복 테스트 시 이전 테스트에서 RESERVED된 좌석이 쌓여 예약 성공률이 낮아짐
# 테스트에 사용하는 좌석(scheduleId=393447)만 초기화해 일관된 결과 보장
echo "DB 초기화 중..."
mysql -uapplication -papplication -h127.0.0.1 hhplus -e "
  UPDATE seat SET status = 'AVAILABLE' WHERE concert_schedule_id = 393447;
  DELETE FROM reservation WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED', 'EXPIRED');
  DELETE FROM payment WHERE reservation_id NOT IN (SELECT id FROM reservation);
" 2>/dev/null
echo "DB 초기화 완료"

# ── k6 실행 ───────────────────────────────────────────────────────────────
echo "k6 테스트 시작: $SCRIPT"
/opt/homebrew/bin/k6 run --out influxdb=http://localhost:8086/k6 "k6/$SCRIPT"
