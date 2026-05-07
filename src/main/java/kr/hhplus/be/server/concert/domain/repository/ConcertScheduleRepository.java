package kr.hhplus.be.server.concert.domain.repository;

import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {

    @Query(
            value = "select id, concert_id, concert_date, status " +
                    "from concert_schedule " +
                    "where concert_id = :concertId " +
                    "and status = 'AVAILABLE' " +
                    "and concert_date >= SYSDATE() " +
                    "order by concert_date asc",
            countQuery = "select count(*) from concert_schedule " +
                    "where concert_id = :concertId " +
                    "and status = 'AVAILABLE' " +
                    "and concert_date >= SYSDATE()",
            nativeQuery = true
    )
    Page<ConcertSchedule> findAvailableSchedule(@Param("concertId") Long concertId, Pageable pageable);
}
