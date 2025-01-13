package kr.hhplus.be.server.reservation.application.dto;

import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationResult(
        Long concertId,
        Long reservationId,
        LocalDateTime concertAt,
        Long seatId,
        Long price,
        ReservationStatus status,
        LocalDateTime expiredAt

) {
}