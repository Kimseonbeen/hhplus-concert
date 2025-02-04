package kr.hhplus.be.server.queueToken.infrastructure;

import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.repository.QueueTokenRepository;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QueueTokenRepositoryImpl implements QueueTokenRepository {

    private final QueueTokenJpaRepository queueTokenJpaRepository;

    @Override
    public long countByStatus(QueueTokenStatus status) {
        return queueTokenJpaRepository.countByStatus(status);
    }

    @Override
    public Optional<QueueToken> findByToken(String token) {
        return queueTokenJpaRepository.findByToken(token);
    }

    @Override
    public QueueToken save(QueueToken queueToken) {
        return queueTokenJpaRepository.save(queueToken);
    }

    @Override
    public long countByStatusAndIdLessThan(QueueTokenStatus status, long userId) {
        return queueTokenJpaRepository.countByStatusAndIdLessThan(status, userId);
    }

    @Override
    public List<QueueToken> findByStatusAndExpiredAtBefore(QueueTokenStatus status, LocalDateTime dateTime) {
        return queueTokenJpaRepository.findByStatusAndExpiredAtBefore(status, dateTime);
    }

    @Override
    public Optional<QueueToken> findFirstByStatusOrderByIdAsc(QueueTokenStatus status) {
        return queueTokenJpaRepository.findFirstByStatusOrderByIdAsc(status);
    }

    @Override
    public Optional<QueueToken> findByUserId(Long userId) {
        return queueTokenJpaRepository.findByUserId(userId);
    }
}
