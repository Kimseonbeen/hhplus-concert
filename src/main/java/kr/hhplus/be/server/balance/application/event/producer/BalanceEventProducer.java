package kr.hhplus.be.server.balance.application.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishBalanceDecrease(ReservationPendingEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("balance-decrease", event.getUserId().toString(), eventJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }
}
