package kr.hhplus.be.server.balance.application.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.balance.application.event.producer.BalanceEventProducer;
import kr.hhplus.be.server.balance.domain.service.BalanceService;
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
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reservation-pending", groupId = "balance-service-group")
    public void listen(ConsumerRecord<String, String> record) {

        try {
            ReservationPendingEvent event = objectMapper.readValue(record.value(), ReservationPendingEvent.class);

            // 역직렬화된 객체로 서비스 호출
            balanceService.decrease(event.getUserId(), event.getAmount());

            // 이벤트 발행
            eventProducer.publishBalanceDecrease(event);

            log.info("Received Message: topic - {}, partition - {}, offset - {}, key - {}, value - {}",
                    record.topic(), record.partition(), record.offset(),
                    record.key(), record.value());
        } catch (JsonProcessingException e) {
            log.error("JSON 역직렬화 오류: {}", e.getMessage(), e);
            // 예외 처리 - 필요에 따라 데드 레터 큐로 전송하거나 재시도 로직 구현
        }
    }

    // 잔액 롤백 처리 컨슈머 추가
    @KafkaListener(topics = "balance-rollback", groupId = "balance-rollback-group")
    public void handleRollback(ConsumerRecord<String, String> record) {
        try {
            // 롤백 이벤트 파싱
            ReservationResultEvent event = objectMapper.readValue(record.value(), ReservationResultEvent.class);

            // 잔액 환불(증가) 처리
            balanceService.increase(event.getUserId(), event.getAmount());

            log.info("잔액 롤백 완료: userId={}, amount={}, reservationId={}",
                    event.getUserId(), event.getAmount(), event.getReservationId());

            // 선택적: 롤백 완료 이벤트 발행
            //eventProducer.publishBalanceRollbackCompleted(event);

        } catch (JsonProcessingException e) {
            log.error("롤백 이벤트 파싱 오류: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("잔액 롤백 처리 실패: {}", e.getMessage(), e);
            //수동 개입이 필요한 상황
        }
    }
}
