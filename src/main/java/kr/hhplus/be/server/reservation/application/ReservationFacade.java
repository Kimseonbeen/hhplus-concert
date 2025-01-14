package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatAvailabilityInfo;
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
        
        // 좌석 예약 가능 여부 확인
        SeatAvailabilityInfo seatAvailabilityInfo = concertService.validateAndReservationInfo(
                command.scheduleId(),
                command.seatId()
        );
        
        // 예약 실행
        Reservation reservation = processReservation(seatAvailabilityInfo, command.userId());

        // 응답 생성
        return ReservationResult.builder()
                .reservationId(reservation.getId())
                .concertId(seatAvailabilityInfo.concertId())
                .concertAt(seatAvailabilityInfo.concertDate())
                .seatId(seatAvailabilityInfo.seatId())
                .price(seatAvailabilityInfo.price())
                .status(reservation.getStatus())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

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

    private Reservation processReservation(SeatAvailabilityInfo seatAvailabilityInfo, Long userId) {
        Reservation reservation = reservationService.createReservation(
                seatAvailabilityInfo.seatId(),
                seatAvailabilityInfo.price(),
                userId
        );

        concertService.occupySeat(seatAvailabilityInfo.seatId());

        return reservation;
    }
}
