package kr.hhplus.be.server.concert.domain.model;

import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeatTest {

    @Test
    @DisplayName("이미 예약된 좌석을 예약 시도하면 SEAT_ALREADY_OCCUPIED 에러가 발생한다")
    void isAvailable_WhenSeatOccupied_ThrowsException() {
        // given
        Seat seat = Seat.builder()
                .status(SeatStatus.RESERVED)
                .build();

        // when
        ConcertException concertException = assertThrows(ConcertException.class, seat::reserved);

        // then
        assertEquals(concertException.getMessage(), ConcertErrorCode.SEAT_ALREADY_OCCUPIED.getMsg());

    }

    @Test
    @DisplayName("예약 가능한 좌석을 예약하면 상태가 RESERVED로 변경된다")
    void reserved_ChangeToReservedStatus() {
        // given
        Seat seat = Seat.builder()
                .status(SeatStatus.AVAILABLE)
                .build();

        // when
        seat.reserved();

        // then
        assertEquals(seat.getStatus(), SeatStatus.RESERVED);
    }

}