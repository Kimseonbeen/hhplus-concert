package kr.hhplus.be.server.balance.domain.model;

import jakarta.persistence.*;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Builder
public class BalanceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balance_id")
    private Long balanceId;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private BalanceHistoryType type;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
