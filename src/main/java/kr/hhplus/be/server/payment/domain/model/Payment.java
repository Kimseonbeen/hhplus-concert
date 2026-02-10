package kr.hhplus.be.server.payment.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
