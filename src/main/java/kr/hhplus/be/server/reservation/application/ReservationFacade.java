package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.concert.domain.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.ConcertService;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.queueToken.domain.QueueTokenService;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationFacade {
    private final ConcertService concertService;
    private final ReservationService reservationService;

    @Transactional
    public ReservationResult reserve(ReservationCommand command) {

        // 1. 공연과 좌석 정보 조회
        ConcertSchedule schedule = concertService.getSchedule(command.scheduleId());
        Seat seat = concertService.getSeat(command.seatId());

        // 2. 예약 가능 상태 검증
        concertService.validateReservationAvailability(schedule, seat);

        // 3. 예약 생성
        Reservation reservation = reservationService.createReservation(seat,command.userId());

        // 4. 좌석 임시 점유
        concertService.occupySeat(seat);

        // 5. 응답 생성
        return ReservationResult.builder()
                .reservationId(reservation.getId())
                .concertId(schedule.getConcertId())
                .concertAt(schedule.getConcertDate())
                .seat(seat)
                .status(reservation.getStatus())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
