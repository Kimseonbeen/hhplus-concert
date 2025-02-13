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
    public Long getWaitingTokenCount() {
        return queueTokenJpaRepository.countByStatus(QueueTokenStatus.WAITING);
    }

    @Override
    public Long getActiveTokenCount() {
        return queueTokenJpaRepository.countByStatus(QueueTokenStatus.ACTIVE);
    }

    @Override
    public Optional<QueueToken> findByToken(String token) {
        return queueTokenJpaRepository.findByToken(token);
    }

    @Override
    public void save(QueueToken queueToken) {
        queueTokenJpaRepository.save(queueToken);
    }

    @Override
    public Long countWaitingAhead(QueueTokenStatus status, long userId) {
        return queueTokenJpaRepository.countByStatusAndIdLessThan(status, userId);
    }

    @Override
    public List<QueueToken> findExpiredTokens(QueueTokenStatus status, LocalDateTime dateTime) {
        return queueTokenJpaRepository.findByStatusAndExpiredAtBefore(status, dateTime);
    }

    @Override
    public Optional<QueueToken> getNextToken(QueueTokenStatus status) {
        return queueTokenJpaRepository.findFirstByStatusOrderByIdAsc(status);
    }

    @Override
    public Optional<QueueToken> findByUserId(Long userId) {
        return queueTokenJpaRepository.findByUserId(userId);
    }

    @Override
    public List<String> getWaitingTokens(Long needs) {
        return List.of();
    }

    @Override
    public void saveAcviveTokens(String s) {

    }

    @Override
    public void removeWaitingTokens(List<String> waitingTokens) {

    }

    @Override
    public void removeToken(String token) {

    }
}
