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

    // 동시에 여러 사용자가 같은 좌석을 예약 시도할 때의 동시성 제어는
    // 여기서 직접 처리하지 않고 Seat의 @Version(낙관적 락)에 위임한다.
    // update 시점에 버전이 어긋나면 OptimisticLockException이 발생하고,
    // 이 예외는 호출부(ReservationFacade)에서 SEAT_RESERVATION_CONFLICT로 변환해
    // 클라이언트가 "다른 사람이 선점함"을 알고 재시도하도록 안내한다.
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
