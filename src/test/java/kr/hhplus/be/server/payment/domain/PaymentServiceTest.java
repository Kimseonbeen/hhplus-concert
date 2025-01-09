package kr.hhplus.be.server.payment.domain;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatRepository;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.queueToken.domain.QueueToken;
import kr.hhplus.be.server.queueToken.domain.QueueTokenRepository;
import kr.hhplus.be.server.queueToken.domain.QueueTokenStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("유효한 결제 요청 시 결제가 성공적으로 처리된다")
    void processPayment_Success() {
        // given
        Long reservationId = 1L;
        Long userId = 1L;
        int amount = 100000;

        Payment expectedPayment = Payment.builder()
                .id(1L)
                .userId(userId)
                .reservationId(reservationId)
                .amount(amount)
                .build();

        given(paymentRepository.save(any(Payment.class))).willReturn(expectedPayment);

        // when
        Payment result = paymentService.processPayment(reservationId, userId, amount);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(reservationId, result.getReservationId());
        assertEquals(amount, result.getAmount());
        verify(paymentRepository).save(any(Payment.class));
    }
}