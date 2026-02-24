package kr.hhplus.be.server.reservation.application.event;

import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import kr.hhplus.be.server.reservation.infrastructure.client.DataPlatformClient;
import kr.hhplus.be.server.reservation.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventListener {
    private final DataPlatformClient dataPlatformClient;
    private final QueueTokenService queueTokenService;

    // outbox 저장
    @TransactionalEventListener(phase = BEFORE_COMMIT)
    public void saveOutbox(PaymentCompletedEvent event) {
        //orderCreatedEventRepository.save(orderCreatedEvent)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        // 커밋 완료 후 토큰 만료 처리 (트랜잭션 롤백 영향 없음)
        queueTokenService.expireToken(event.getToken());

        try {
            // 데이터 플랫폼 API 호출
            dataPlatformClient.sendReservationData(event.getPaymentId());
        } catch (Exception e) {
            log.error("데이터 플랫폼 API 호출 실패", e);
        }
    }
}
