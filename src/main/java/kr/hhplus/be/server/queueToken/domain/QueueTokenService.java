package kr.hhplus.be.server.queueToken.domain;

import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenError;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenErrorCode;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueueTokenService {

    private final QueueTokenRepository queueTokenRepository;

    @Transactional
    public QueueToken issueQueueToken(long userId) {

        // ACTIVE 토큰 확인
        long activeCount = queueTokenRepository.countByStatus(QueueTokenStatus.ACTIVE);
        // WAITING 토큰 확인 == 대기순서 확인
        long waitingCount = queueTokenRepository.countByStatus(QueueTokenStatus.WAITING);

        QueueToken queueToken = QueueToken.issueToken(userId, activeCount, waitingCount);

        queueTokenRepository.save(queueToken);

        return queueToken;
    }


    public QueueTokenResponse getQueueToken(String token) {

        QueueToken queueToken = queueTokenRepository.findByToken(token);

        // 토큰 상태가 waiting
        if (queueToken.isWaiting()) {
            long waitingNum = queueTokenRepository.countByStatusAndIdLessThan(
                    QueueTokenStatus.WAITING,
                    queueToken.getId()
            );
            return QueueTokenResponse.of(queueToken, waitingNum+1);
        }

        return QueueTokenResponse.of(queueToken, 0L);
    }

    public void validateToken(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token);

        if (queueToken.isExpired()) {
            throw new QueueTokenError(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND);
        }

        if (!queueToken.isActive()) {
            throw new QueueTokenError(QueueTokenErrorCode.QUEUE_TOKEN_NOT_ACTIVE);
        }
    }
}
