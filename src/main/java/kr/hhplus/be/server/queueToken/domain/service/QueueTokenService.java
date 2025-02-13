package kr.hhplus.be.server.queueToken.domain.service;

import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenException;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenErrorCode;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import kr.hhplus.be.server.queueToken.domain.repository.QueueTokenRepository;
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
    private static final long MAX_ACTIVE_TOKEN_COUNT = 150;

    @Transactional
    public QueueToken issueQueueToken(long userId) {

        // ACTIVE 토큰 확인
        long activeCount = queueTokenRepository.getActiveTokenCount();
        // WAITING 토큰 확인 == 대기순서 확인
        long waitingCount = queueTokenRepository.getWaitingTokenCount();

        QueueToken queueToken = QueueToken.issueToken(userId, activeCount, waitingCount);

        queueTokenRepository.save(queueToken);

        return queueToken;
    }

    public QueueTokenResponse getQueueToken(String token) {

        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new QueueTokenException(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND));

        return QueueTokenResponse.of(queueToken);
    }

    public void validateToken(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new QueueTokenException(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND));

        if (queueToken.isExpired()) {
            throw new QueueTokenException(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND);
        }

        if (!queueToken.isActive()) {
            throw new QueueTokenException(QueueTokenErrorCode.QUEUE_TOKEN_NOT_ACTIVE);
        }
    }

    // 토큰 만료 처리 (결제 완료 이후)
    public void expireToken(String token) {
        queueTokenRepository.removeToken(token);
    }



    // 대기 토큰 활성화
    public void activateNextWaitingToken() {

        Long activeTokenCount = queueTokenRepository.getActiveTokenCount();

        if (activeTokenCount < MAX_ACTIVE_TOKEN_COUNT) {
            long needs = MAX_ACTIVE_TOKEN_COUNT - activeTokenCount;

            // needs 만큼 waitingToken 조회
            List<String> waitingTokens = queueTokenRepository.getWaitingTokens(needs);

            if (!waitingTokens.isEmpty()) {

                // 조회한 waitingToken 삭제
                queueTokenRepository.removeWaitingTokens(waitingTokens);

                // activeToken 생성
                waitingTokens.forEach(queueTokenRepository::saveAcviveTokens);
            }
        }
    }
}
