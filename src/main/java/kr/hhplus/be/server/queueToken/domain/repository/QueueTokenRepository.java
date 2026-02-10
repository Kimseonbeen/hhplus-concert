package kr.hhplus.be.server.queueToken.domain.repository;

import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;

import java.util.Optional;

public interface QueueTokenRepository {
    Long getWaitingTokenCount();
    Long getActiveTokenCount();
    Optional<QueueToken> findByToken(String token);
    void save(QueueToken queueToken);
    Optional<QueueToken> getNextToken(QueueTokenStatus status);
    Optional<QueueToken> findByUserId(Long userId);
    void removeToken(String token);
    Long atomicallyActivateWaitingTokens(long needs);
}
