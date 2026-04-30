package kr.hhplus.be.server.reservation.application.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import kr.hhplus.be.server.reservation.domain.event.ReservationResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishReservationPending(ReservationPendingEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("reservation-pending", event.getUserId().toString(), eventJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }

    public void publishReservationCompleted(ReservationResultEvent event) throws JsonProcessingException {
        String eventJson = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("reservation-completed", event.getUserId().toString(), eventJson);
    }

    // 예약 완료 로직이 실패 ! 잔액을 롤백해줘야함
    public void publishReservationFailed(ReservationResultEvent event) throws JsonProcessingException {
        String eventJson = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("balance-rollback", event.getUserId().toString(), eventJson);
    }
}
