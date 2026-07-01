package kr.hhplus.be.server.concert.domain.service;

import kr.hhplus.be.server.concert.domain.model.*;
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;

    public Page<ConcertScheduleResponse> getConcertSchedules(Long concertId, Pageable pageable) {
        Page<ConcertSchedule> schedules = concertScheduleRepository.findAvailableSchedule(concertId, pageable);
        if (schedules.isEmpty()) {
            throw new ConcertException(ConcertErrorCode.CONCERT_NOT_FOUND);
        }
        return schedules.map(ConcertScheduleResponse::from);
    }

    public ConcertSeatAvailableResponse getAvailableSeats(Long concertScheduleId, Pageable pageable) {
        ConcertSchedule schedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new ConcertException(ConcertErrorCode.CONCERT_NOT_FOUND));

        schedule.checkIsAvailable();

        Page<Integer> availableSeats = seatRepository.findByConcertScheduleIdAndStatus(
                concertScheduleId,
                SeatStatus.AVAILABLE,
                pageable
        );

        return ConcertSeatAvailableResponse.from(schedule, availableSeats);
    }

    @Transactional
    public SeatResult reserveSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ConcertException(ConcertErrorCode.SEAT_NOT_FOUND));

        seat.reserved();

        ConcertSchedule schedule = concertScheduleRepository.findById(seat.getConcertScheduleId())
                .orElseThrow(() -> new ConcertException(ConcertErrorCode.CONCERT_NOT_FOUND));

        return SeatResult.from(seat, schedule);
    }

    @Transactional
    public void releaseSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ConcertException(ConcertErrorCode.SEAT_NOT_FOUND));
        seat.release();
    }

}
