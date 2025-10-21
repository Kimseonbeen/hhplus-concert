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

    public static BalanceHistory createHistory(Long amount, Balance balance, BalanceHistoryType balanceHistoryType) {
        return BalanceHistory.builder()
                .balanceId(balance.getId())
                .amount(amount)          // 양수 그대로 저장
                .type(balanceHistoryType)
                .build();
    }
}
