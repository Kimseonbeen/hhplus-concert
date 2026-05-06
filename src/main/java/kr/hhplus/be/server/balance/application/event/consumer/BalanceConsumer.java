package kr.hhplus.be.server.balance.application.event.consumer;

import kr.hhplus.be.server.balance.application.event.producer.BalanceEventProducer;
import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.common.serializer.DataSerializer;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import kr.hhplus.be.server.reservation.domain.event.ReservationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceConsumer {

    private final BalanceService balanceService;
    private final BalanceEventProducer eventProducer;

    @KafkaListener(topics = "reservation-pending", groupId = "balance-service-group")
    public void listen(ConsumerRecord<String, String> record) {
        ReservationPendingEvent event = DataSerializer.deserialize(record.value(), ReservationPendingEvent.class);

        try {
            balanceService.decrease(event.getUserId(), event.getAmount());
            eventProducer.publishBalanceDecrease(event);

            log.info("잔액 차감 완료: topic={}, userId={}", record.topic(), event.getUserId());
        } catch (Exception e) {
            log.error("잔액 차감 실패: userId={}", event.getUserId(), e);
        }
    }

    @KafkaListener(topics = "balance-rollback", groupId = "balance-rollback-group")
    public void handleRollback(ConsumerRecord<String, String> record) {
        ReservationResultEvent event = DataSerializer.deserialize(record.value(), ReservationResultEvent.class);

        try {
            balanceService.increase(event.getUserId(), event.getAmount());
            log.info("잔액 롤백 완료: userId={}, amount={}", event.getUserId(), event.getAmount());
        } catch (Exception e) {
            log.error("잔액 롤백 실패 — 수동 개입 필요: userId={}", event.getUserId(), e);
        }
    }
}
