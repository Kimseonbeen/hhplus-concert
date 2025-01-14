package kr.hhplus.be.server.concert.domain.repository;

import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByConcertScheduleIdAndStatus(Long concertScheduleId, SeatStatus status);
}
