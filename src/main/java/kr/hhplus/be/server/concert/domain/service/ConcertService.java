package kr.hhplus.be.server.concert.domain.service;

import kr.hhplus.be.server.concert.domain.model.*;
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;

    public List<ConcertScheduleResponse> getConcertSchedules(Long concertId) {
        List<ConcertScheduleResponse> schedules = concertScheduleRepository.findByConcertId(concertId)
                .stream()
                .filter(concertSchedule -> concertSchedule.getStatus() == ConcertScheduleStatus.AVAILABLE)
                .filter(ConcertSchedule::isDateAvailable)
                .map(ConcertScheduleResponse::from)
                .toList();

        if (schedules.isEmpty()) {
            throw new ConcertException(ConcertErrorCode.CONCERT_NOT_FOUND);
        }

        return schedules;
    }

    public ConcertSeatAvailableResponse getAvailableSeats(Long concertScheduleId) {
        // 공연 일정 조회
        ConcertSchedule schedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new ConcertException(ConcertErrorCode.CONCERT_NOT_FOUND));

        // 예약 가능한 좌석 조회
        List<Integer> availableSeats = seatRepository.findByConcertScheduleIdAndStatus(
                        concertScheduleId,
                        SeatStatus.AVAILABLE
                )
                .stream()
                .map(Seat::getSeatNum)
                .collect(Collectors.toList());

        return ConcertSeatAvailableResponse.builder()
                .date(schedule.getConcertDate())
                .availableSeats(availableSeats)
                .build();
    }

    public SeatResult reserveSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ConcertException(ConcertErrorCode.SEAT_NOT_FOUND));

        seat.reserved();

        return SeatResult.from(seatRepository.save(seat));
    }
}
