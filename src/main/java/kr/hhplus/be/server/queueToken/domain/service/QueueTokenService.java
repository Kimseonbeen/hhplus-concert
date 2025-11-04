package kr.hhplus.be.server.queueToken.domain.service;

import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenException;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenErrorCode;
import kr.hhplus.be.server.queueToken.domain.model.QueueConstants;
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
    private static final long MAX_ACTIVE_TOKEN_COUNT = QueueConstants.MAX_ACTIVE_USERS;

    @Transactional
    public QueueToken createToken(Long userId) {
        // ACTIVE 토큰 확인
        Long activeCount = queueTokenRepository.getActiveTokenCount();

        // 서비스 계층에서 status 결정
        QueueTokenStatus status = (activeCount < QueueConstants.MAX_ACTIVE_USERS) ?
                QueueTokenStatus.ACTIVE : QueueTokenStatus.WAITING;

        // 도메인 팩토리에 필요한 최소 정보만 전달
        QueueToken queueToken = QueueToken.createToken(userId, status);

        queueTokenRepository.save(queueToken);

        return queueToken;
    }

    public QueueTokenResponse getQueueToken(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new QueueTokenException(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND));

        return QueueTokenResponse.from(queueToken);
    }

    public void validateToken(String token) {
        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new QueueTokenException(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND));

        if (queueToken.isExpired()) {
            throw new QueueTokenException(QueueTokenErrorCode.QUEUE_TOKEN_EXPIRED);
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

            // [핵심 변경] 원자적 전환 메서드 호출
            long activatedCount = queueTokenRepository.atomicallyActivateWaitingTokens(needs);

            // 활성화된 토큰 개수 로깅 등 후속 작업 수행
            if (activatedCount > 0) {
                log.info("[QueueTokenService.activateNextWaitingToken : successfully tokens activated {} waiting tokens]", activatedCount);
            }
        }
    }
}
