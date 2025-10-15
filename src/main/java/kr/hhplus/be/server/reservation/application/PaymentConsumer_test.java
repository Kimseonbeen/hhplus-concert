package kr.hhplus.be.server.reservation.application;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentConsumer_test {
    @KafkaListener(topics = "payment-topic", groupId = "hhplus-group")
    public void listen(ConsumerRecord<String, String> record) {
        log.info("Received Message: topic - {}, partition - {}, offset - {}, key - {}, value - {}",
                record.topic(), record.partition(), record.offset(),
                record.key(), record.value());
    }
}