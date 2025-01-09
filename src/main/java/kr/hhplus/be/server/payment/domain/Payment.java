package kr.hhplus.be.server.payment.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
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
    private Integer amount;

    @Column
    private LocalDateTime createdAt;

    public static Payment createPayment(long reservationId, Long userId, int amount) {
        return Payment.builder()
                .reservationId(reservationId)
                .userId(userId)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
