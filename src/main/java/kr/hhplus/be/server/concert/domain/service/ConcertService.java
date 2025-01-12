package kr.hhplus.be.server.concert.domain.service;

import kr.hhplus.be.server.concert.domain.repository.ConcertRepository;
import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.exception.ConcertError;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.model.Seat;
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

    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;

    public List<ConcertScheduleResponse> getConcertSchedules(long concertId) {
        ConcertSchedule schedule = concertRepository.findById(concertId)
                .orElseThrow(() -> new ConcertError(ConcertErrorCode.CONCERT_NOT_FOUND));

        return List.of(ConcertScheduleResponse.builder()
                .concertScheduleId(schedule.getId())
                .concertDate(schedule.getConcertDate())
                .build());
    }

    @Transactional(readOnly = true)
    public ConcertSeatAvailableResponse getAvailableSeats(Long concertScheduleId) {
        // 공연 일정 조회
        ConcertSchedule schedule = concertRepository.findById(concertScheduleId)
                .orElseThrow(() -> new ConcertError(ConcertErrorCode.CONCERT_NOT_FOUND));

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

    public ConcertSchedule getSchedule(Long concertScheduleId) {
        return concertRepository.findById(concertScheduleId)
                .orElseThrow(() -> new ConcertError(ConcertErrorCode.CONCERT_NOT_FOUND));
    }

    public Seat getSeat(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new ConcertError(ConcertErrorCode.SEAT_NOT_FOUND));
    }

    public void validateReservationAvailability(ConcertSchedule schedule, Seat seat) {
        schedule.isAvailable();
        seat.isAvailable();
    }

    public void occupySeat(Seat seat) {
        seat.occupy();
    }

    public void updateSeatStatus(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ConcertError(ConcertErrorCode.SEAT_NOT_FOUND));

        seat.reserved();
    }
}
