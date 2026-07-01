package kr.hhplus.be.server.concert.domain.repository;

import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.model.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    @Query(
            value = "select seat_num from seat " +
                    "where seat.concert_schedule_id = :concertScheduleId " +
                    "and status = :status",
            countQuery = "select count(*) from seat " +
                    "where concert_schedule_id = :concertScheduleId " +
                    "and status = :status",
            nativeQuery = true
    )
    Page<Integer> findByConcertScheduleIdAndStatus(
            @Param("concertScheduleId") Long concertScheduleId,
            @Param("status") SeatStatus status,
            Pageable pageable
    );
}
