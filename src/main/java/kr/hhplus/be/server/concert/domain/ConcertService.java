package kr.hhplus.be.server.concert.domain;

import kr.hhplus.be.server.common.exception.CustomException;
import kr.hhplus.be.server.concert.domain.exception.ConcertError;
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

    private final ConcertRepository ConcertRepository;
    private final SeatRepository seatRepository;

    public List<ConcertScheduleResponse> getConcertSchedules(long concertScheduleId) {
        ConcertSchedule schedule = ConcertRepository.findById(concertScheduleId)
                .orElseThrow(() -> new ConcertError(ConcertErrorCode.CONCERT_NOT_FOUND));

        return List.of(ConcertScheduleResponse.builder()
                .concertScheduleId(schedule.getId())
                .concertDate(schedule.getConcertDate())
                .build());
    }

    @Transactional(readOnly = true)
    public ConcertSeatAvailableResponse getAvailableSeats(Long concertScheduleId) {
        // 공연 일정 조회
        ConcertSchedule schedule = ConcertRepository.findById(concertScheduleId)
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
}
