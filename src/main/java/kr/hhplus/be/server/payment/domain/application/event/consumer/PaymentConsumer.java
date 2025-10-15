package kr.hhplus.be.server.payment.domain.application.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.balance.application.event.producer.BalanceEventProducer;
import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.payment.domain.application.event.producer.PaymentEventProducer;
import kr.hhplus.be.server.payment.domain.service.PaymentService;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import kr.hhplus.be.server.reservation.domain.event.ReservationResultEvent;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {
    private final PaymentService paymentService;
    private final PaymentEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reservation-completed", groupId = "payment-service-group")
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {

        ReservationPendingEvent event = objectMapper.readValue(record.value(), ReservationPendingEvent.class);

        try {
            // 역직렬화된 객체로 서비스 호출
            paymentService.processPayment(event.getReservationId(), event.getUserId(), event.getAmount());

            // 이벤트 발행
            //eventProducer.publishBalanceDecrease(event);

            log.info("Received Message: topic - {}, partition - {}, offset - {}, key - {}, value - {}",
                    record.topic(), record.partition(), record.offset(),
                    record.key(), record.value());
        } catch (Exception e) {
            // 보상 로직 실행 ( 결제 생성 로직이 실패 했기에, 실패 이벤트를 발행하고, 이전 예약완료 및 잔액감소 롤백이 이루어져야한다. )
            ReservationResultEvent failEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.PENDING_PAYMENT
            );

            // 예약 완료 실패 이벤트 발행
            eventProducer.publishReservationFailed(failEvent);
        }
    }
}
