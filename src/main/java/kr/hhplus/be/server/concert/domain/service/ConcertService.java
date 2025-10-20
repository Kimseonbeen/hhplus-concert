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

import java.util.List;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertScheduleRepository concertScheduleRepository;
    private final SeatRepository seatRepository;

    public List<ConcertScheduleResponse> getConcertSchedules(Long concertId) {

        List<ConcertScheduleResponse> schedules = concertScheduleRepository.findAvailableSchedule(concertId)
                .stream()
                .map(ConcertScheduleResponse::from)
                .toList();

        if (schedules.isEmpty()) {
            throw new ConcertException(ConcertErrorCode.CONCERT_NOT_FOUND);
        }

        return schedules;
    }

    public ConcertSeatAvailableResponse getAvailableSeats(Long concertScheduleId) {
        // 공연 일정 조회
        // 공연이 아직 종료가 안 되어있어야함
        ConcertSchedule schedule = concertScheduleRepository.findById(concertScheduleId)
                .orElseThrow(() -> new ConcertException(ConcertErrorCode.CONCERT_NOT_FOUND));

        // 방어로직
        schedule.checkIsAvailable();

        /**
         * 기존 소스
         * List<Integer> availableSeats = seatRepository.findByConcertScheduleIdAndStatus(
         *                         concertScheduleId,
         *                         SeatStatus.AVAILABLE
         *                 )
         *                 .stream()
         *                 .map(Seat::getSeatNum) <-- 메모리에서 추출
         *                 .collect(toList());
         * 변경이유 : 애플리케이션 서버에서 데이터를 받은 후, JPA(Hibernate 등)는 전송된 모든 컬럼 데이터를 사용하여 불필요한 Seat 엔티티 객체 인스턴스를 메모리에 생성하고 있는데,
         * 필요 없는 엔티티 객체를 임시로 생성하므로 메모리가 불필요하게 사용됨
         */
        List<Integer> availableSeats = seatRepository.findByConcertScheduleIdAndStatus(
                concertScheduleId,
                SeatStatus.AVAILABLE
        );

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
