package kr.hhplus.be.server.queueToken.infrastructure;

import jakarta.annotation.PostConstruct;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import kr.hhplus.be.server.queueToken.domain.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Primary
@RequiredArgsConstructor
@Slf4j
public class QueueTokenRedisRepositoryImpl implements QueueTokenRepository {

    private static final String WAITING_TOKEN_PREFIX = "waiting-token";
    private static final String ACTIVE_TOKEN_PREFIX = "active-token";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);
    private final RedisTemplate<String, String> redisTemplate;
    private DefaultRedisScript<Long> activateTokensScript;

    @PostConstruct
    public void init() {
        this.activateTokensScript = new DefaultRedisScript<>();
        this.activateTokensScript.setResultType(Long.class);

        // ResourceScriptSource를 사용하여 ClassPathResource를 감싸면 타입 불일치 문제 해결
        this.activateTokensScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("scripts/activate_tokens.lua"))
        );

        log.info("Redis Lua Script 'activate_tokens.lua' has been successfully loaded.");
    }

    @Override
    public Long getActiveTokenCount() {
        Set<String> keys = redisTemplate.keys(ACTIVE_TOKEN_PREFIX + ":*");
        return (long) keys.size();
    }

    @Override
    public Long getWaitingTokenCount() {
        return redisTemplate.opsForZSet().size(WAITING_TOKEN_PREFIX);
    }

    @Override
    public Optional<QueueToken> findByToken(String token) {
        // 1. ACTIVE 상태 확인
        String activeToken = redisTemplate.opsForValue().get(ACTIVE_TOKEN_PREFIX + ":" + token);

        if (activeToken != null) {
            return Optional.of(QueueToken.builder()
                    .token(token)
                    .position(0L)
                    .status(QueueTokenStatus.ACTIVE)
                    .build());
        }

        // 2. WAITING 상태 확인 및 대기 순서 조회
        Double waitingScore = redisTemplate.opsForZSet().score(WAITING_TOKEN_PREFIX, token);

        if (waitingScore != null) {
            Long position = redisTemplate.opsForZSet().rank(WAITING_TOKEN_PREFIX, token);
            return Optional.of(QueueToken.builder()
                    .token(token)
                    .status(QueueTokenStatus.WAITING)
                    .position(position)
                    .build());
        }

        // 3. 토큰을 찾지 못한 경우
        return Optional.empty();
    }

    @Override
    public void save(QueueToken queueToken) {
        String token = queueToken.getToken();
        if (queueToken.getStatus() == QueueTokenStatus.ACTIVE) {
            redisTemplate.opsForValue().set(ACTIVE_TOKEN_PREFIX + ":" + token, token, TOKEN_TTL);
        } else {
            redisTemplate.opsForZSet().add(WAITING_TOKEN_PREFIX, token, System.currentTimeMillis());
        }
    }

    @Override
    public void removeToken(String token) {
        redisTemplate.delete(ACTIVE_TOKEN_PREFIX + ":" + token);
    }

    @Override
    public Long atomicallyActivateWaitingTokens(long needs) {
        // 1. 스크립트에 전달할 Redis 키 (KEYS[1], KEYS[2])
        List<String> keys = Arrays.asList(
                WAITING_TOKEN_PREFIX,  // KEYS[1]: ZSET 키
                ACTIVE_TOKEN_PREFIX    // KEYS[2]: String PREFIX
        );

        // 2. 스크립트에 전달할 인자 (ARGV[1], ARGV[2])
        long activeTtlSeconds = 60 * 10; // 10분 TTL (600초)

        // 3. Lua 스크립트 실행 (RTT 1회, 원자성 보장)
        return redisTemplate.execute(
                activateTokensScript,
                keys,   // List[keys]
                String.valueOf(needs),  // ARGV[1]
                String.valueOf(activeTtlSeconds)    // ARGV[2]
        );
    }

    @Override
    public Optional<QueueToken> getNextToken(QueueTokenStatus status) {
        return Optional.empty();
    }

    @Override
    public Optional<QueueToken> findByUserId(Long userId) {
        return Optional.empty();
    }
}
