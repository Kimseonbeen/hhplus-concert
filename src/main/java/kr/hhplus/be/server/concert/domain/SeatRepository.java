package kr.hhplus.be.server.concert.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByConcertScheduleIdAndStatus(Long concertScheduleId, SeatStatus status);
}
