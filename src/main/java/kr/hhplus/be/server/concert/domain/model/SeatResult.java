package kr.hhplus.be.server.concert.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SeatResult(
        Long scheduleId,
        LocalDateTime concertDate,
        Long concertId,
        Long seatId,
        Integer seatNum,
        Long price

) {

    public static SeatResult from(Seat seat) {
        return SeatResult.builder()
                .seatId(seat.getId())
                .seatNum(seat.getSeatNum())
                .price(seat.getPrice())
                .build();
    }

}
