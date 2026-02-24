package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatResult;
import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.service.ConcertService;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.service.PaymentService;
import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import kr.hhplus.be.server.reservation.application.dto.PaymentCommand;
import kr.hhplus.be.server.reservation.application.dto.PaymentResult;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationFacadeTest {

    @Mock
    private ConcertService concertService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private BalanceService balanceService;

    @Mock
    private QueueTokenService queueTokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservationFacade reservationFacade;

    @Test
    @DisplayName("좌석 예약 시 예약 정보가 올바르게 반환된다")
    void reserve_Success() {
        // given
        Long userId = 1L;
        Long seatId = 5L;
        Long scheduleId = 1L;

        ReservationCommand command = ReservationCommand.builder()
                .userId(userId)
                .scheduleId(scheduleId)
                .seatId(seatId)
                .build();

        SeatResult seatResult = SeatResult.builder()
                .scheduleId(scheduleId)
                .concertId(1L)
                .concertDate(LocalDateTime.of(2026, 3, 1, 18, 0))
                .seatId(seatId)
                .seatNum(15)
                .price(50000L)
                .build();

        Reservation reservation = Reservation.builder()
                .id(42L)
                .userId(userId)
                .seatId(seatId)
                .price(50000L)
                .status(ReservationStatus.PENDING_PAYMENT)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        given(concertService.reserveSeat(seatId)).willReturn(seatResult);
        given(reservationService.createReservation(userId, seatId, 50000L)).willReturn(reservation);

        // when
        ReservationResult result = reservationFacade.reserve(command);

        // then
        assertNotNull(result);
        assertEquals(42L, result.reservationId());
        assertEquals(userId, result.userId());
        assertEquals(seatId, result.seatId());
        assertEquals(50000L, result.price());
        assertEquals(ReservationStatus.PENDING_PAYMENT, result.status());
    }

    @Test
    @DisplayName("결제 시 클라이언트 금액이 아닌 DB에 저장된 예약 금액으로 결제된다")
    void completePayment_UsesReservationPrice_NotClientAmount() {
        // given
        Long reservationId = 42L;
        Long userId = 1L;
        Long reservationPrice = 50000L;  // DB에 저장된 실제 금액
        String token = "test-token";

        PaymentCommand command = PaymentCommand.builder()
                .reservationId(reservationId)
                .userId(userId)
                .build();  // amount 없음

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .price(reservationPrice)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        Payment payment = Payment.builder()
                .id(10L)
                .userId(userId)
                .reservationId(reservationId)
                .amount(reservationPrice)
                .build();

        given(reservationService.getReservation(reservationId)).willReturn(reservation);
        given(paymentService.processPayment(reservationId, userId, reservationPrice)).willReturn(payment);

        // when
        PaymentResult result = reservationFacade.completePayment(command, token);

        // then
        assertNotNull(result);
        assertEquals(reservationPrice, result.amount());

        // 검증: DB에서 가져온 금액(50000)으로 잔액 차감
        verify(balanceService).decrease(userId, reservationPrice);

        // 검증: DB에서 가져온 금액(50000)으로 결제 처리
        verify(paymentService).processPayment(reservationId, userId, reservationPrice);
    }

    @Test
    @DisplayName("결제 완료 후 토큰이 만료된다")
    void completePayment_ExpiresToken_AfterPayment() {
        // given
        Long reservationId = 42L;
        Long userId = 1L;
        String token = "test-token";

        PaymentCommand command = PaymentCommand.builder()
                .reservationId(reservationId)
                .userId(userId)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .price(50000L)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        Payment payment = Payment.builder()
                .id(10L)
                .userId(userId)
                .reservationId(reservationId)
                .amount(50000L)
                .build();

        given(reservationService.getReservation(reservationId)).willReturn(reservation);
        given(paymentService.processPayment(any(), any(), any())).willReturn(payment);

        // when
        reservationFacade.completePayment(command, token);

        // then
        verify(queueTokenService).expireToken(token);
    }
}
