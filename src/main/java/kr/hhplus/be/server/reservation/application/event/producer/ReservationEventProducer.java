package kr.hhplus.be.server.reservation.application.event.producer;

import kr.hhplus.be.server.common.serializer.DataSerializer;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import kr.hhplus.be.server.reservation.domain.event.ReservationResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishReservationPending(ReservationPendingEvent event) {
        kafkaTemplate.send("reservation-pending", event.getUserId().toString(), DataSerializer.serialize(event));
    }

    public void publishReservationCompleted(ReservationResultEvent event) {
        kafkaTemplate.send("reservation-completed", event.getUserId().toString(), DataSerializer.serialize(event));
    }

    public void publishReservationFailed(ReservationResultEvent event) {
        kafkaTemplate.send("balance-rollback", event.getUserId().toString(), DataSerializer.serialize(event));
    }
}
