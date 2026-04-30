package kr.hhplus.be.server.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // 기존 트랜잭션에 참여 (결제 완료 시 같이 커밋) — outboxId 반환
    @Transactional
    public Long save(OutboxEventType eventType, Map<String, Object> payload) {
        OutboxEvent saved = outboxEventRepository.save(OutboxEvent.create(eventType, toJson(payload)));
        return saved.getId();
    }

    // 리스너에서 즉시 전송 성공 시 PUBLISHED로 변경
    @Transactional
    public void publish(Long outboxId) {
        outboxEventRepository.findById(outboxId)
                .ifPresent(OutboxEvent::publish);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("아웃박스 payload 직렬화 실패", e);
        }
    }
}
