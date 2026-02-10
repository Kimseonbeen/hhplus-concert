package kr.hhplus.be.server.reservation.domain.service;

import kr.hhplus.be.server.reservation.application.dto.PaymentCommand;
import kr.hhplus.be.server.reservation.domain.event.ReservationPendingEvent;
import kr.hhplus.be.server.reservation.domain.exception.ReservationError;
import kr.hhplus.be.server.reservation.domain.exception.ReservationErrorCode;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    private final ApplicationEventPublisher eventPublisher;

    public Reservation createReservation(Long userId, Long seatId, Long price) {
        // 2. 새로운 예약 생성
        Reservation reservation = Reservation.createReservation(seatId, price, userId);

        // 3. 예약 정보 저장
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void pendingReservation(PaymentCommand command, String token) {
        // 이벤트 생성
        ReservationPendingEvent event = new ReservationPendingEvent(
                command.reservationId(),
                command.userId(),
                command.amount(),
                token
        );
        eventPublisher.publishEvent(event);
        //eventProducer.publishReservationPending(event);
    }

    @Transactional
    public void completeReserve(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.PENDING_PAYMENT)
                .orElseThrow(() -> new ReservationError(ReservationErrorCode.RESERVATION_NOT_FOUND));

        reservation.complete();
    }

    @Transactional
    public void failReserve(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.CONFIRMED)
                .orElseThrow(() -> new ReservationError(ReservationErrorCode.RESERVATION_NOT_FOUND));

        reservation.fail();
    }
}
