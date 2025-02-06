package kr.hhplus.be.server.queueToken.infrastructure;

import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import kr.hhplus.be.server.queueToken.domain.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@Primary
@RequiredArgsConstructor
@Slf4j
public class QueueTokenRedisRepositoryImpl implements QueueTokenRepository {

    private static final String WAITING_TOKEN_PREFIX = "waiting-token";
    private static final String ACTIVE_TOKEN_PREFIX = "active-token";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Long getWaitingTokenCount() {
        return redisTemplate.opsForZSet().size(WAITING_TOKEN_PREFIX);
    }

    @Override
    public Long getActiveTokenCount() {
        Set<String> keys = redisTemplate.keys(ACTIVE_TOKEN_PREFIX + ":*");
        return (long) keys.size();
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
    public Long countWaitingAhead(QueueTokenStatus status, long userId) {
        Long rank = redisTemplate.opsForZSet().rank(status.toString(), userId);
        return rank != null ? rank : 0L;
    }

    @Override
    public List<String> getWaitingTokens(Long needs) {
        Set<String> tokens = redisTemplate.opsForZSet().range(WAITING_TOKEN_PREFIX, 0, needs);

        if (tokens != null) return tokens.stream().toList();

        return List.of();
    }

    @Override
    public void removeWaitingTokens(List<String> tokens) {
        redisTemplate.opsForZSet().remove(WAITING_TOKEN_PREFIX, tokens.toArray());
    }

    @Override
    public void removeToken(String token) {
        redisTemplate.delete(token);
    }

    @Override
    public void saveAcviveTokens(String token) {
        redisTemplate.opsForValue().set(ACTIVE_TOKEN_PREFIX + ":" + token, token);
        redisTemplate.expire(ACTIVE_TOKEN_PREFIX + ":" + token, 10, TimeUnit.MINUTES);
    }

    // 처리 필요...
    @Override
    public List<QueueToken> findExpiredTokens(QueueTokenStatus status, LocalDateTime dateTime) {
        return List.of();
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
