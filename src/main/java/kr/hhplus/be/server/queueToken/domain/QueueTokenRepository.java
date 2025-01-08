package kr.hhplus.be.server.queueToken.domain;

public interface QueueTokenRepository {
    long countByStatus(QueueTokenStatus status);
    QueueToken findByToken(String token);
    QueueToken save(QueueToken queueToken);
    long countByStatusAndIdLessThan(QueueTokenStatus status, long userId);
}
