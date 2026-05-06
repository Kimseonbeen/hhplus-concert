package kr.hhplus.be.server.common.outbox;

import kr.hhplus.be.server.common.serializer.DataSerializer;
import kr.hhplus.be.server.reservation.infrastructure.client.DataPlatformClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final DataPlatformClient dataPlatformClient;

    @Scheduled(fixedDelay = 10 * 1000) // 10초마다
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllByStatus(OutboxStatus.PENDING);

        for (OutboxEvent event : pendingEvents) {
            try {
                process(event);
                event.publish();
            } catch (Exception e) {
                log.error("아웃박스 이벤트 처리 실패: id={}, type={}", event.getId(), event.getEventType(), e);
                event.fail(e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
    @Transactional
    public void deletePublishedEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        outboxEventRepository.deleteByStatusAndPublishedAtBefore(OutboxStatus.PUBLISHED, threshold);
        log.info("7일 지난 PUBLISHED 아웃박스 이벤트 삭제 완료");
    }

    private void process(OutboxEvent event) throws Exception {
        Map payload = DataSerializer.deserialize(event.getPayload(), Map.class);

        switch (event.getEventType()) {
            case DATA_PLATFORM_SEND -> {
                Long paymentId = Long.valueOf(payload.get("paymentId").toString());
                dataPlatformClient.sendReservationData(paymentId);
                log.info("데이터 플랫폼 전송 완료: paymentId={}", paymentId);
            }
        }
    }
}
