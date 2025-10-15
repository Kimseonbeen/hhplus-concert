package kr.hhplus.be.server.reservation.application.event;

import kr.hhplus.be.server.reservation.application.event.producer.ReservationEventProducer;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationCreatedEventListener {
    private final ReservationEventProducer reservationProducer; // 인터페이스 주입

    @EventListener
    public void handleReservationCreated(ReservationPendingEvent event) {
        // 이벤트 매핑
        ReservationPendingEvent pendingEvent = new ReservationPendingEvent(
                event.getReservationId(),
                event.getUserId(),
                event.getAmount(),
                event.getToken()
        );

        // 프로듀서 인터페이스를 통해 이벤트 발행
        reservationProducer.publishReservationPending(pendingEvent);
    }
}