package kr.hhplus.be.server.reservation.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.Seat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column
    private ReservationStatus status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    public static Reservation createReservation(Seat seat, Long userId) {
        return Reservation.builder()
                .userId(userId)
                .seatId(seat.getId())
                .price(seat.getPrice())
                .status(ReservationStatus.PENDING_PAYMENT)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
