package kr.hhplus.be.server.queueToken.domain.repository;

import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository {
    Long getWaitingTokenCount();
    Long getActiveTokenCount();
    Optional<QueueToken> findByToken(String token);
    void save(QueueToken queueToken);
    Long countWaitingAhead(QueueTokenStatus status, long userId);
    List<QueueToken> findExpiredTokens(QueueTokenStatus status, LocalDateTime dateTime);
    Optional<QueueToken> getNextToken(QueueTokenStatus status);
    Optional<QueueToken> findByUserId(Long userId);
    List<String> getWaitingTokens(Long needs);
    void saveAcviveTokens(String s);
    void removeWaitingTokens(List<String> waitingTokens);
    void removeToken(String token);
}
