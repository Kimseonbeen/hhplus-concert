package kr.hhplus.be.server.concert.domain.model;

import kr.hhplus.be.server.concert.domain.exception.ConcertError;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeatTest {

    @Test
    @DisplayName("좌석이 이미 점유되어 있으면 ConcertErrorCode를 반환한다.")
    void isAvailable_WhenSeatOccupied_ThrowsException() {
        // given
        Seat seat = Seat.builder()
                .status(SeatStatus.TEMPORARY)
                .build();

        // when
        ConcertError concertError = assertThrows(ConcertError.class, seat::isAvailable);

        // then
        assertEquals(concertError.getMessage(), ConcertErrorCode.SEAT_ALREADY_OCCUPIED.getMsg());

    }

    @Test
    @DisplayName("좌석의 상태를 임시예약 상태로 변경한다.")
    void occpuy_ChangeToTemporaryStatus() {
        // given
        Seat seat = Seat.builder()
                .status(SeatStatus.AVAILABLE)
                .build();

        // when
        seat.occupy();

        // then
        assertEquals(seat.getStatus(), SeatStatus.TEMPORARY);
    }

    @Test
    @DisplayName("좌석의 상태를 결제 상태로 변경한다.")
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