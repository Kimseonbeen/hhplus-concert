package kr.hhplus.be.server.payment.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId;

    @Column
    private Long reservationId;

    @Column
    private Long amount;

    @Column
    private LocalDateTime createdAt;

    public static Payment createPayment(long reservationId, Long userId, Long amount) {
        return Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
