package kr.hhplus.be.server.reservation.domain;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("유효한 좌석과 사용자 정보로 예약이 성공적으로 생성된다")
    void createReservation_Success() {
        // given
        Long userId = 1L;
        Seat seat = Seat.builder()
                .id(1L)
                .seatNum(1)
                .price(100000)
                .status(SeatStatus.AVAILABLE)
                .build();

        Reservation expectedReservation = Reservation.builder()
                .id(1L)
                .userId(userId)
                .seatId(seat.getId())
                .price(seat.getPrice())
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        given(reservationRepository.save(any(Reservation.class))).willReturn(expectedReservation);

        // when
        Reservation result = reservationService.createReservation(seat, userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(seat.getId(), result.getSeatId());
        assertEquals(seat.getPrice(), result.getPrice());
        assertEquals(ReservationStatus.PENDING_PAYMENT, result.getStatus());
        verify(reservationRepository).save(any(Reservation.class));
    }
}