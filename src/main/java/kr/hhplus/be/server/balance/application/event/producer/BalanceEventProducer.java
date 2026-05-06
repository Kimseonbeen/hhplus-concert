package kr.hhplus.be.server.balance.application.event.producer;

import kr.hhplus.be.server.common.serializer.DataSerializer;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishBalanceDecrease(ReservationPendingEvent event) {
        kafkaTemplate.send("balance-decrease", event.getUserId().toString(), DataSerializer.serialize(event));
    }
}
