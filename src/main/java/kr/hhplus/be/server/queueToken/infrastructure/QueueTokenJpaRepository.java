package kr.hhplus.be.server.queueToken.infrastructure;

import kr.hhplus.be.server.queueToken.domain.QueueToken;
import kr.hhplus.be.server.queueToken.domain.QueueTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueTokenJpaRepository extends JpaRepository<QueueToken, Long> {
    long countByStatus(QueueTokenStatus status);
    Optional<QueueToken> findByToken(String token);
    long countByStatusAndIdLessThan(QueueTokenStatus status, long userId);
    List<QueueToken> findByStatusAndExpiredAtBefore(QueueTokenStatus status, LocalDateTime dateTime);
    Optional<QueueToken> findFirstByStatusOrderByIdAsc(QueueTokenStatus status);
}
