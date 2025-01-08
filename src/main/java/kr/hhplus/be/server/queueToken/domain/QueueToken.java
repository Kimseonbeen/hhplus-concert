package kr.hhplus.be.server.queueToken.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueToken {

    @Id @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column
    @Enumerated(EnumType.STRING)
    private QueueTokenStatus status;

    @Column
    private LocalDateTime expiredAt;

    public static QueueToken issueToken(Long userId, Long activeCount, Long waitingCount) {
        String token = UUID.randomUUID().toString();
        QueueTokenStatus status = (activeCount < QueueConstants.MAX_ACTIVE_USERS && waitingCount == 0) ?
                QueueTokenStatus.ACTIVE : QueueTokenStatus.WAITING;

        return QueueToken.builder()
                .token(token)
                .userId(userId)
                .status(status)
                .expiredAt((status == QueueTokenStatus.ACTIVE) ? LocalDateTime.now().plusMinutes(10) : null)
                .build();
    }

    public boolean isWaiting() {
        return this.status == QueueTokenStatus.WAITING;
    }
}
