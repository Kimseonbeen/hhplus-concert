package kr.hhplus.be.server.reservation.application.dto;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationResult(
        Long concertId,
        Long reservationId,
        LocalDateTime concertAt,
        Seat seat,
        ReservationStatus status,
        LocalDateTime expiredAt

) {
}