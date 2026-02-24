package kr.hhplus.be.server.reservation.domain.repository;

import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByIdAndStatus(Long id, ReservationStatus status);
    List<Reservation> findAllByStatusAndExpiredAtBefore(ReservationStatus status, LocalDateTime now);
}
