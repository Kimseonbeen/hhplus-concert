package kr.hhplus.be.server.reservation.application.event.consumer;

import kr.hhplus.be.server.common.serializer.DataSerializer;
import kr.hhplus.be.server.reservation.application.event.producer.ReservationEventProducer;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import kr.hhplus.be.server.reservation.domain.event.ReservationResultEvent;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConsumer {

    private final ReservationService reservationService;
    private final ReservationEventProducer eventProducer;

    @KafkaListener(topics = "balance-decrease", groupId = "reservation-service-group")
    public void listen(ConsumerRecord<String, String> record) {
        ReservationPendingEvent event = DataSerializer.deserialize(record.value(), ReservationPendingEvent.class);

        try {
            reservationService.completeReserve(event.getReservationId());

            ReservationResultEvent successEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.CONFIRMED
            );
            eventProducer.publishReservationCompleted(successEvent);

            log.info("예약 완료: topic={}, reservationId={}", record.topic(), event.getReservationId());
        } catch (Exception e) {
            ReservationResultEvent failEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.PENDING_PAYMENT
            );
            eventProducer.publishReservationFailed(failEvent);
            log.error("예약 완료 실패 — 롤백 이벤트 발행: reservationId={}", event.getReservationId(), e);
        }
    }

    @KafkaListener(topics = "reservation-rollback", groupId = "reservation-rollback-group")
    public void handleRollback(ConsumerRecord<String, String> record) {
        ReservationResultEvent event = DataSerializer.deserialize(record.value(), ReservationResultEvent.class);

        try {
            reservationService.failReserve(event.getReservationId());

            ReservationResultEvent failEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.PENDING_PAYMENT
            );
            eventProducer.publishReservationFailed(failEvent);

            log.info("예약 롤백 완료: reservationId={}", event.getReservationId());
        } catch (Exception e) {
            log.error("예약 롤백 실패 — 수동 개입 필요: reservationId={}", event.getReservationId(), e);
        }
    }
}
