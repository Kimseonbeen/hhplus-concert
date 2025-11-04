-- File: src/main/resources/scripts/activate_tokens.lua

-- KEYS[1]: WAITING_TOKEN_PREFIX (대기열 ZSET 키)
-- KEYS[2]: ACTIVE_TOKEN_PREFIX (활성 토큰 String의 접두사)
-- ARGV[1]: needs (활성화할 토큰 개수)
-- ARGV[2]: TTL (활성 토큰의 만료 시간, 초 단위)

local needs = tonumber(ARGV[1])
local ttl_seconds = tonumber(ARGV[2])

-- 1. 대기열(ZSET)에서 'needs' 개수만큼 가장 낮은 점수 멤버를 가져오면서 삭제 (원자적 POP)
-- tokens_and_scores는 {토큰1, 점수1, 토큰2, 점수2, ...} 형태로 반환됩니다.
-- ZPOPMIN은 Redis 5.0 이상에서 사용 가능
local tokens_and_scores = redis.call('ZPOPMIN', KEYS[1], needs)

-- 2. 토큰 목록이 비어 있으면 처리된 토큰 없음(0) 반환
if #tokens_and_scores == 0 then
    return 0
end

-- Lua 스크립트에서 tokens_and_scores 변수에 저장된 배열의 구조 결과는
-- '멤버와 점수가 번갈아 나오는' 하나의 평면 배열(Flat Array) 형태로 반환

-- 인덱스,저장된 값,의미
--    1,토큰 A,멤버 (Token)
--    2,점수 A,토큰 A의 점수 (Score)
--    3,토큰 B,멤버 (Token)
--    4,점수 B,토큰 B의 점수 (Score)
--    5,토큰 C,멤버 (Token)
--    6,점수 C,토큰 C의 점수 (Score)

-- 3. 활성 토큰으로 변환 (TTL 설정)
-- 루프는 토큰만 사용하기 위해 2단계씩 건너뜁니다.
for i = 1, #tokens_and_scores, 2 do
    local token = tokens_and_scores[i]
    -- ..은 Lua 언어에서 문자열을 연결하는 연산자
    local active_key = KEYS[2] .. ":" .. token

    -- SET key value EX seconds (SET과 EXPIRE를 원자적으로 수행)
    redis.call('SET', active_key, token, 'EX', ttl_seconds)
end

-- 4. 처리된 토큰 개수 반환
return #tokens_and_scores / 2