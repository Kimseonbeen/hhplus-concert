package kr.hhplus.be.server.queueToken.presentation.scheduler;

import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueTokenScheduler {

    private final QueueTokenService queueTokenService;

    @Scheduled(fixedDelay = 1000)
    public void QueueTokenStatusChange() {
        log.info("start schedule");

        try {
            // 1. 만료된 ACTIVE 토큰 찾아서 EXPIRED로 변경
            List<QueueToken> expiredTokens = queueTokenService.findExpiredActiveTokens();

            for (QueueToken expiredToken : expiredTokens) {
                log.info("Expired token processing - Token: {}", expiredToken.getToken());

                // 2. 토큰 만료 처리
                queueTokenService.expireToken(expiredToken);

                // 3. 다음 WAITING 토큰을 ACTIVE로 변경
                queueTokenService.activateNextWaitingToken();
            }
        } catch (Exception e) {
            log.error("대기열 토큰 상태 변경 중 오류 발생", e);
        }

    }
}
