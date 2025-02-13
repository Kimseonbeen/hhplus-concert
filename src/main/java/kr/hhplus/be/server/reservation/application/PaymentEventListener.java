package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.reservation.infrastructure.client.DataPlatformClient;
import kr.hhplus.be.server.reservation.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private final DataPlatformClient dataPlatformClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            // 데이터 플랫폼 API 호출
            dataPlatformClient.sendReservationData(event.getPaymentId());
        } catch (Exception e) {
            log.error("데이터 플랫폼 API 호출 실패", e);
        }
    }
}
