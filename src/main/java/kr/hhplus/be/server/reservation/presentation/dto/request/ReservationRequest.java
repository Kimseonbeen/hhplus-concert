package kr.hhplus.be.server.reservation.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "좌석 예약 요청")
public record ReservationRequest(
        long concertId,
        LocalDateTime date,
        int seatNum
) {
}
