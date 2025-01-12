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
        Long reservationId,
        Long concertId,
        LocalDateTime concertAt,
        SeatResponse seat,
        ReservationStatus reservationStatus,
        LocalDateTime expiredAt
) {
        public static ReservationResponse from(ReservationResult reservation) {
                return ReservationResponse.builder()
                        .reservationId(reservation.reservationId())
                        .concertId(reservation.concertId())
                        .concertAt(reservation.concertAt())
                        .seat(SeatResponse.builder()
                                .seatId(reservation.seat().getId())
                                //.seatNo(reservation.seat().getSeatNum())
                                .seatPrice(reservation.seat().getPrice())
                                .build())
                        .reservationStatus(reservation.status())
                        .expiredAt(reservation.expiredAt())
                        .build();
        }

}