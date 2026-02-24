package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.balance.domain.service.BalanceService;
import jakarta.persistence.OptimisticLockException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.model.SeatResult;
import kr.hhplus.be.server.concert.domain.service.ConcertService;
import kr.hhplus.be.server.reservation.domain.event.PaymentCompletedEvent;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.service.PaymentService;
import kr.hhplus.be.server.reservation.application.dto.PaymentCommand;
import kr.hhplus.be.server.reservation.application.dto.PaymentResult;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationFacade {

    private final ApplicationEventPublisher eventPublisher;

    private final ConcertService concertService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final BalanceService balanceService;

    @Transactional
    public ReservationResult reserve(ReservationCommand command) {
        try {
            // 좌석 예약
            SeatResult seatResult = concertService.reserveSeat(command.seatId());

            // 예약 생성
            Reservation reservation = reservationService.createReservation(command.userId(), seatResult.seatId(), seatResult.price());

            // 응답 생성
            return ReservationResult.of(reservation, seatResult);
        } catch (OptimisticLockException e) {
            throw new ConcertException(ConcertErrorCode.SEAT_RESERVATION_CONFLICT);
        }
    }

    @Transactional
    public PaymentResult completePayment(PaymentCommand command, String token) {

        // 예약 조회 및 금액 확인 (클라이언트 금액 미사용)
        Reservation reservation = reservationService.getReservation(command.reservationId());
        Long amount = reservation.getPrice();

        // 잔액 감소
        balanceService.decrease(command.userId(), amount);

        // 예약 완료
        reservationService.completeReserve(command.reservationId());

        // 결제 처리
        Payment payment = paymentService.processPayment(
                command.reservationId(),
                command.userId(),
                amount
        );

        // 예약 결제 완료 이벤트 발송 (AFTER_COMMIT에서 토큰 만료 처리)
        eventPublisher.publishEvent(new PaymentCompletedEvent(payment.getId(), token));

        return PaymentResult.from(payment);
    }
}
