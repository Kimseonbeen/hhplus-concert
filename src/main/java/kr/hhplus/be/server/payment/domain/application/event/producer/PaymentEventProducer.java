package kr.hhplus.be.server.payment.domain.application.event.producer;

import kr.hhplus.be.server.common.serializer.DataSerializer;
import kr.hhplus.be.server.reservation.domain.event.ReservationResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishReservationFailed(ReservationResultEvent event) {
        kafkaTemplate.send("reservation-rollback", event.getUserId().toString(), DataSerializer.serialize(event));
    }
}
