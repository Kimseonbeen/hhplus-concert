package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatResult;
import kr.hhplus.be.server.concert.domain.service.ConcertService;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.service.PaymentService;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import kr.hhplus.be.server.reservation.application.dto.PaymentCommand;
import kr.hhplus.be.server.reservation.application.dto.PaymentResult;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationFacade {

    private final ConcertService concertService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final BalanceService balanceService;
    private final QueueTokenService queueTokenService;

    @Transactional
    public ReservationResult reserve(ReservationCommand command) {
        
        // 좌석 예약
        SeatResult seatResult = concertService.reserveSeat(command.seatId());
        
        // 예약 생성
        reservationService.createReservation(command.userId(), seatResult.seatId(), seatResult.price());

        // 응답 생성
        return ReservationResult.of(command.userId(), seatResult.seatId(), seatResult.price());
    }

    @Transactional
    public PaymentResult completePayment(PaymentCommand command) {

        // 잔액 감소
        balanceService.decrease(command.userId(), command.amount());
        
        // 예약 완료
        reservationService.completeReserve(command.reservationId());

        // 결제 처리
        Payment payment = paymentService.processPayment(
                command.reservationId(),
                command.userId(),
                command.amount()
        );

        // 토큰 만료 처리
        queueTokenService.expireToken(command.userId());

        return PaymentResult.from(payment);
    }
}
