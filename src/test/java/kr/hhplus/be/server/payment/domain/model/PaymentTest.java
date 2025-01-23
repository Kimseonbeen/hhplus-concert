package kr.hhplus.be.server.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    @DisplayName("Payment 생성 시 예약ID, 사용자ID, 결제금액이 저장되고 생성시간이 현재시간으로 설정된다")
    void createPayment_ShouldCreateWithCorrectValues() {
        // given
        long reservationId = 1L;
        Long userId = 100L;
        Long amount = 50000L;
        LocalDateTime beforeCreate = LocalDateTime.now();

        // when
        Payment payment = Payment.createPayment(reservationId, userId, amount);

        // then
        assertEquals(reservationId, payment.getReservationId());
        assertEquals(userId, payment.getUserId());
        assertEquals(amount, payment.getAmount());
        assertTrue(payment.getCreatedAt().isAfter(beforeCreate) ||
                payment.getCreatedAt().isEqual(beforeCreate));
        assertTrue(payment.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
}