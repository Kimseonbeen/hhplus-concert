package kr.hhplus.be.server.queueToken.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository {
    long countByStatus(QueueTokenStatus status);
    QueueToken findByToken(String token);
    QueueToken save(QueueToken queueToken);
    long countByStatusAndIdLessThan(QueueTokenStatus status, long userId);
    List<QueueToken> findByStatusAndExpiredAtBefore(QueueTokenStatus status, LocalDateTime dateTime);
    Optional<QueueToken> findFirstByStatusOrderByIdAsc(QueueTokenStatus status);

}
