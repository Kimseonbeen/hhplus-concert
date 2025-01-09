package kr.hhplus.be.server.queueToken.domain;

import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenError;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenErrorCode;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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

    public QueueToken findToken(String token) {
        return queueTokenRepository.findByToken(token);
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

    // 만료된 ACTIVE 토큰 조회
    public List<QueueToken> findExpiredActiveTokens() {
        return queueTokenRepository.findByStatusAndExpiredAtBefore(
                QueueTokenStatus.ACTIVE,
                LocalDateTime.now()
        );
    }

    // 토큰 만료 처리
    public void expireToken(QueueToken token) {
        token.expire();
        queueTokenRepository.save(token);
    }

    // 대기 토큰 활성화

    /**
     * findFirstByStatusOrderByIdAsc Query
     * SELECT * FROM queue_token
     * WHERE status = ?
     * ORDER BY id ASC
     * LIMIT 1
     */
    public void activateNextWaitingToken() {
        queueTokenRepository.findFirstByStatusOrderByIdAsc(QueueTokenStatus.WAITING)
                .ifPresent(token -> {
                    token.activate();  // 상태만 ACTIVE로 변경
                    queueTokenRepository.save(token);
                });
    }
}
