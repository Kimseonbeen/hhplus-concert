package kr.hhplus.be.server.concert.domain.repository;

import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {

    List<ConcertSchedule> findByConcertId(long concertId);
}
