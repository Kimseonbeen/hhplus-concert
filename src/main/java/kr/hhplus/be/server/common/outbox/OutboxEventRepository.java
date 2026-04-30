package kr.hhplus.be.server.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByStatus(OutboxStatus status);

    void deleteByStatusAndPublishedAtBefore(OutboxStatus status, LocalDateTime threshold);
}
