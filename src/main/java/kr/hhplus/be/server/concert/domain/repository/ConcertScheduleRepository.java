package kr.hhplus.be.server.concert.domain.repository;

import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {

    List<ConcertSchedule> findByConcertId(long concertId);

    @Query(
            value = "select id, concert_id, concert_date, status " +
                    "from concert_schedule " +
                    "where concert_id = :concertId" +
                    "and status = 'AVAILABLE' " +
                    "and concert_date >= SYSDATE();",
            nativeQuery = true
    )
    List<ConcertSchedule> findAvailableSchedule(Long concertId);
}
