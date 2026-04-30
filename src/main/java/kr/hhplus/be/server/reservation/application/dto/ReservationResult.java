package kr.hhplus.be.server.reservation.application.dto;

import kr.hhplus.be.server.concert.domain.model.SeatResult;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationResult(
        Long reservationId,
        Long userId,
        Long seatId,
        Integer seatNum,
        Long concertId,
        LocalDateTime concertAt,
        Long price,
        ReservationStatus status,
        LocalDateTime expiredAt
) {
    public static ReservationResult of(Reservation reservation, SeatResult seatResult) {
        return ReservationResult.builder()
                .reservationId(reservation.getId())
                .userId(reservation.getUserId())
                .seatId(seatResult.seatId())
                .seatNum(seatResult.seatNum())
                .concertId(seatResult.concertId())
                .concertAt(seatResult.concertDate())
                .price(seatResult.price())
                .status(reservation.getStatus())
                .expiredAt(reservation.getExpiredAt())
                .build();
    }
}