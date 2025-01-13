package kr.hhplus.be.server.reservation.domain.service;

import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.reservation.domain.exception.ReservationError;
import kr.hhplus.be.server.reservation.domain.exception.ReservationErrorCode;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public Reservation createReservation(Long seatId, BigDecimal price, Long userId) {
        // 2. 새로운 예약 생성
        Reservation reservation = Reservation.createReservation(seatId, price, userId);

        // 3. 예약 정보 저장
        return reservationRepository.save(reservation);
    }

    public Reservation getPendingReservation(Long reservationId) {
        return reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.PENDING_PAYMENT)
                .orElseThrow(() -> new ReservationError(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    public void updateReservationStatus(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndStatus(reservationId, ReservationStatus.PENDING_PAYMENT)
                .orElseThrow(() -> new ReservationError(ReservationErrorCode.RESERVATION_NOT_FOUND));

        reservation.complete();
    }
}
