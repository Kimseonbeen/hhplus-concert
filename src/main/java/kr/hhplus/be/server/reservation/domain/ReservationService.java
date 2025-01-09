package kr.hhplus.be.server.reservation.domain;

import kr.hhplus.be.server.concert.domain.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public Reservation createReservation(Seat seat, Long userId) {
        // 2. 새로운 예약 생성
        Reservation reservation = Reservation.createReservation(seat, userId);

        // 3. 예약 정보 저장
        return reservationRepository.save(reservation);
    }
}
