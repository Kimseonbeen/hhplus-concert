package kr.hhplus.be.server.reservation.domain;

import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.repository.ReservationRepository;
import kr.hhplus.be.server.reservation.domain.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
                .price(100000L)
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
        Reservation result = reservationService.createReservation(seat.getId(), seat.getPrice(), userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(seat.getId(), result.getSeatId());
        assertEquals(seat.getPrice(), result.getPrice());
        assertEquals(ReservationStatus.PENDING_PAYMENT, result.getStatus());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("결제 대기 상태의 예약을 완료 상태로 변경한다")
    void completeReserve_WhenPendingPayment_ChangeStatusToConfirmed() {
        // given
        Long reservationId = 1L;
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        given(reservationRepository.findByIdAndStatus(
                reservationId,
                ReservationStatus.PENDING_PAYMENT
        )).willReturn(Optional.of(reservation));

        // when
        reservationService.completeReserve(reservationId);

        // then
        assertEquals(reservation.getStatus(), ReservationStatus.CONFIRMED);
        verify(reservationRepository).findByIdAndStatus(reservationId, ReservationStatus.PENDING_PAYMENT);
    }
}