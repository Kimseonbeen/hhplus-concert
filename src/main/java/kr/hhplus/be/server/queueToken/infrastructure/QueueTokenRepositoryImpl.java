package kr.hhplus.be.server.queueToken.infrastructure;

import kr.hhplus.be.server.queueToken.domain.QueueToken;
import kr.hhplus.be.server.queueToken.domain.QueueTokenRepository;
import kr.hhplus.be.server.queueToken.domain.QueueTokenStatus;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenError;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QueueTokenRepositoryImpl implements QueueTokenRepository {

    private final QueueTokenJpaRepository queueTokenJpaRepository;

    @Override
    public long countByStatus(QueueTokenStatus status) {
        return queueTokenJpaRepository.countByStatus(status);
    }

    @Override
    public QueueToken findByToken(String token) {
        return queueTokenJpaRepository.findByToken(token)
                .orElseThrow(() -> new QueueTokenError(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND));
    }

    @Override
    public QueueToken save(QueueToken queueToken) {
        return queueTokenJpaRepository.save(queueToken);
    }

    @Override
    public long countByStatusAndIdLessThan(QueueTokenStatus status, long userId) {
        return queueTokenJpaRepository.countByStatusAndIdLessThan(status, userId);
    }
}
