package kr.hhplus.be.server.queueToken.presentation.scheduler;

import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueTokenScheduler {

    private final QueueTokenService queueTokenService;

    private static final int ACTIVATION_INTERVAL = 30;

    @Scheduled(fixedDelay = ACTIVATION_INTERVAL * 1000)
    public void queueTokenStatusChange() {
        log.info("start schedule");

        try {
            queueTokenService.activateNextWaitingToken();
            log.info("[QueueTokenScheduler.QueueTokenStatusChange]");
        } catch (Exception e) {
            log.error("대기열 토큰 상태 변경 중 오류 발생", e);
        }

    }
}
