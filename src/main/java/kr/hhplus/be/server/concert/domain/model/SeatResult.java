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

    public static SeatResult from(Seat seat, ConcertSchedule schedule) {
        return SeatResult.builder()
                .scheduleId(schedule.getId())
                .concertDate(schedule.getConcertDate())
                .concertId(schedule.getConcertId())
                .seatId(seat.getId())
                .seatNum(seat.getSeatNum())
                .price(seat.getPrice())
                .build();
    }

}
