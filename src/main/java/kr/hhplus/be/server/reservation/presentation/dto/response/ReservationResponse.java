package kr.hhplus.be.server.reservation.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.concert.presentation.dto.response.SeatResponse;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "예약 응답")
public record ReservationResponse(
        @Schema(description = "예약 ID", example = "1")
        Long reservationId,
        @Schema(description = "콘서트 ID", example = "1")
        Long concertId,
        @Schema(description = "콘서트 일시", example = "2026-06-01T18:00:00")
        LocalDateTime concertAt,
        @Schema(description = "좌석 정보")
        SeatResponse seat,
        @Schema(description = "예약 상태 (PENDING/CONFIRMED/CANCELLED)", example = "PENDING")
        ReservationStatus reservationStatus,
        @Schema(description = "예약 만료 시간", example = "2026-06-01T18:05:00")
        LocalDateTime expiredAt
) {
        public static ReservationResponse from(ReservationResult reservation) {
                return ReservationResponse.builder()
                        .reservationId(reservation.reservationId())
                        .concertId(reservation.concertId())
                        .concertAt(reservation.concertAt())
                        .seat(SeatResponse.builder()
                                .seatId(reservation.seatId())
                                .seatNo(reservation.seatNum().longValue())
                                .seatPrice(reservation.price())
                                .build())
                        .reservationStatus(reservation.status())
                        .expiredAt(reservation.expiredAt())
                        .build();
        }

}