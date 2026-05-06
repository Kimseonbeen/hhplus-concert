package kr.hhplus.be.server.payment.domain.application.event.consumer;

import kr.hhplus.be.server.common.serializer.DataSerializer;
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

    @KafkaListener(topics = "reservation-completed", groupId = "payment-service-group")
    public void listen(ConsumerRecord<String, String> record) {
        ReservationPendingEvent event = DataSerializer.deserialize(record.value(), ReservationPendingEvent.class);

        try {
            paymentService.processPayment(event.getReservationId(), event.getUserId(), event.getAmount());

            log.info("결제 처리 완료: topic={}, reservationId={}", record.topic(), event.getReservationId());
        } catch (Exception e) {
            ReservationResultEvent failEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.PENDING_PAYMENT
            );
            eventProducer.publishReservationFailed(failEvent);
            log.error("결제 처리 실패 — 롤백 이벤트 발행: reservationId={}", event.getReservationId(), e);
        }
    }
}
