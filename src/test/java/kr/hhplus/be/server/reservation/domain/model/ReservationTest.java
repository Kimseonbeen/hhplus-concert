package kr.hhplus.be.server.reservation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    @DisplayName("예약 객체가 올바르게 생성된다")
    void createReservation_ShouldCreateWithCorrectValues() {
        // given
        Long seatId = 1L;
        Long price = 50000L;
        Long userId = 100L;

        // when
        Reservation reservation = Reservation.createReservation(seatId, price, userId);

        // then
        assertEquals(seatId, reservation.getSeatId());
        assertEquals(price, reservation.getPrice());
        assertEquals(userId, reservation.getUserId());
        assertEquals(ReservationStatus.PENDING_PAYMENT, reservation.getStatus());
        assertNotNull(reservation.getExpiredAt());
        assertTrue(reservation.getExpiredAt().isAfter(LocalDateTime.now()));
        assertTrue(reservation.getExpiredAt().isBefore(LocalDateTime.now().plusMinutes(6)));
    }

    @Test
    @DisplayName("예약 상태를 CONFIRMED로 변경한다")
    void complete_ShouldChangeStatusToConfirmed() {
        // given
        Reservation reservation = Reservation.builder()
                .seatId(1L)
                .price(50000L)
                .userId(100L)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        // when
        reservation.complete();

        // then
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }


}