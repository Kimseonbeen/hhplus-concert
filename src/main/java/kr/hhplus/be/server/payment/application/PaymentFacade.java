package kr.hhplus.be.server.payment.application;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.concert.domain.service.ConcertService;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.payment.application.dto.PaymentCommand;
import kr.hhplus.be.server.payment.application.dto.PaymentResult;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.service.PaymentService;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final BalanceService balanceService;
    private final ConcertService concertService;
    private final ReservationService reservationService;
    private final QueueTokenService queueTokenService;

    @Transactional
    public PaymentResult completePayment(String token, PaymentCommand command) {

        // 1. 예약 정보 조회
        Reservation reservation = reservationService.getPendingReservation(command.reservationId());

        // 2. 좌석 금액 조회
        Seat seat = concertService.getSeat(reservation.getSeatId());

        // 3. 결제 처리
        Payment payment = paymentService.processPayment(
                command.reservationId(),
                command.userId(),
                seat.getPrice()
        );

        // 4. 금액 변경
        balanceService.decrease(command.userId(), seat.getPrice());
        // 5. 좌석 점유 처리
        concertService.updateSeatStatus(reservation.getSeatId());
        
        // 6. 예약 상태 변경 처리
        reservationService.updateReservationStatus(reservation.getId());

        // 7. 토큰 만료 처리
        QueueToken queueToken = queueTokenService.findToken(token);
        queueTokenService.expireToken(queueToken);

        return PaymentResult.from(payment);
    }

}
