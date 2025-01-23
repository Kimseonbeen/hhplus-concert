package kr.hhplus.be.server.reservation.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    @DisplayName("예약 생성 시 좌석ID, 가격, 사용자ID가 저장되고 상태는 PENDING_PAYMENT로 설정되며 만료시간은 5분 후로 설정된다")
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
    @DisplayName("결제 대기 상태의 예약을 완료 처리하면 상태가 CONFIRMED로 변경된다")
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