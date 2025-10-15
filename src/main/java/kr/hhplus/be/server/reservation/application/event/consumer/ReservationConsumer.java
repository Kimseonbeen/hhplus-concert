package kr.hhplus.be.server.reservation.application.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "balance-decrease", groupId = "reservation-service-group")
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {

        // JSON 문자열을 객체로 역직렬화
        ReservationPendingEvent event = objectMapper.readValue(record.value(), ReservationPendingEvent.class);

        try {
            // 역직렬화된 객체로 서비스 호출
            reservationService.completeReserve(event.getReservationId());

            // 성공 결과 이벤트 생성 및 발행
            ReservationResultEvent successEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.CONFIRMED
            );

            // 이벤트 발행
            eventProducer.publishReservationCompleted(successEvent);

            log.info("이벤트 발행 완료 ReservationConsumer");

            log.info("Received Message: topic - {}, partition - {}, offset - {}, key - {}, value - {}",
                    record.topic(), record.partition(), record.offset(),
                    record.key(), record.value());
        } catch (Exception e) {
            // 보상 로직 실행 ( 예약 완료 로직이 실패 했기에, 실패 이벤트를 발행하고, 잔액 증감이 이루어져야한다. )
            ReservationResultEvent failEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.PENDING_PAYMENT
            );

            // 예약 완료 실패 이벤트 발행
            eventProducer.publishReservationFailed(failEvent);

            log.error("JSON 역직렬화 오류: {}", e.getMessage(), e);
        }
    }

    // 예약 확정 롤백 처리 컨슈머 추가
    @KafkaListener(topics = "reservation-rollback", groupId = "reservation-rollback-group")
    public void handleRollback(ConsumerRecord<String, String> record) {
        try {
            // 롤백 이벤트 파싱
            ReservationResultEvent event = objectMapper.readValue(record.value(), ReservationResultEvent.class);

            // 결제 대기 처리
            reservationService.failReserve(event.getReservationId());

            log.info("결제 대기 완료: userId={}, amount={}, reservationId={}",
                    event.getUserId(), event.getAmount(), event.getReservationId());

            ReservationResultEvent failEvent = new ReservationResultEvent(
                    event.getReservationId(),
                    event.getUserId(),
                    event.getAmount(),
                    ReservationStatus.PENDING_PAYMENT
            );

            log.info("잔액 롤백 실행");
            eventProducer.publishReservationFailed(failEvent);

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
