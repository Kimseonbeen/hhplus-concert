package kr.hhplus.be.server.concert.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record SeatAvailabilityInfo(
        Long scheduleId,
        LocalDateTime concertDate,
        Long concertId,
        Long seatId,
        Integer seatNum,
        BigDecimal price

) {

    public static SeatAvailabilityInfo from(ConcertSchedule schedule, Seat seat) {
        return SeatAvailabilityInfo.builder()
                .scheduleId(schedule.getId())
                .concertDate(schedule.getConcertDate())
                .concertId(schedule.getConcertId())
                .seatId(seat.getId())
                .seatNum(seat.getSeatNum())
                .price(seat.getPrice())
                .build();
    }

}
