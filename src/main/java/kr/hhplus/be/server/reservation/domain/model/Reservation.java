package kr.hhplus.be.server.reservation.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column
    private ReservationStatus status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    public static Reservation createReservation(Long seatId, Long price, Long userId) {
        return Reservation.builder()
                .userId(userId)
                .seatId(seatId)
                .price(price)
                .status(ReservationStatus.PENDING_PAYMENT)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    public void complete() {
        this.status = ReservationStatus.CONFIRMED;
    }
}
