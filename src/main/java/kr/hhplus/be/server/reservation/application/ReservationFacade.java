package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.balance.domain.service.BalanceService;
import jakarta.persistence.OptimisticLockException;
import kr.hhplus.be.server.common.outbox.OutboxEventService;
import kr.hhplus.be.server.common.outbox.OutboxEventType;
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

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationFacade {

    private final ApplicationEventPublisher eventPublisher;

    private final ConcertService concertService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final BalanceService balanceService;
    private final OutboxEventService outboxEventService;

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

        try {
            // 잔액 감소 (REQUIRES_NEW — 독립 트랜잭션 커밋)
            balanceService.decrease(command.userId(), amount);

            // 예약 완료
            reservationService.completeReserve(command.reservationId());

            // 결제 처리
            Payment payment = paymentService.processPayment(
                    command.reservationId(),
                    command.userId(),
                    amount
            );

            // 데이터 플랫폼 전송 아웃박스 저장 (트랜잭션 A와 같이 커밋 — 유실 방지)
            Long outboxId = outboxEventService.save(OutboxEventType.DATA_PLATFORM_SEND,
                    Map.of("paymentId", payment.getId()));

            // 예약 결제 완료 이벤트 발송 (AFTER_COMMIT에서 토큰 만료 + 데이터 플랫폼 전송)
            eventPublisher.publishEvent(new PaymentCompletedEvent(payment.getId(), token, outboxId));

            return PaymentResult.from(payment);

        } catch (Exception e) {
            // 잔액 차감은 이미 커밋됨 → 직접 보상 (REQUIRES_NEW로 독립 커밋)
            log.error("결제 처리 실패 — 잔액 보상: userId={}, amount={}", command.userId(), amount, e);
            balanceService.increase(command.userId(), amount);
            throw e;
        }
    }

    @Transactional
    public void expireOverdueReservations() {
        List<Reservation> expiredReservations = reservationService.findExpiredReservations();

        for (Reservation reservation : expiredReservations) {
            reservationService.expireReservation(reservation.getId());
            concertService.releaseSeat(reservation.getSeatId());
        }
    }
}
