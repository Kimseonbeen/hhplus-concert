package kr.hhplus.be.server.payment.domain.application.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.reservation.domain.event.ReservationResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 결제 생성 로직이 실패 !
    public void publishReservationFailed(ReservationResultEvent event) throws JsonProcessingException {
        String eventJson = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("reservation-rollback", event.getUserId().toString(), eventJson);
    }
}
