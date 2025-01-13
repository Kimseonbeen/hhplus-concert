package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.concert.domain.model.SeatAvailabilityInfo;
import kr.hhplus.be.server.concert.domain.service.ConcertService;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.service.ReservationService;
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
        
        // 좌석 예약 가능 여부 확인
        SeatAvailabilityInfo seatAvailabilityInfo = concertService.validateAndReservationInfo(
                command.scheduleId(),
                command.seatId()
        );
        
        // 예약 실행
        Reservation reservation = processReservation(seatAvailabilityInfo, command.userId());

        // 응답 생성
        return ReservationResult.builder()
                .reservationId(reservation.getId())
                .concertId(seatAvailabilityInfo.concertId())
                .concertAt(seatAvailabilityInfo.concertDate())
                .seatId(seatAvailabilityInfo.seatId())
                .price(seatAvailabilityInfo.price())
                .status(reservation.getStatus())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    private Reservation processReservation(SeatAvailabilityInfo seatAvailabilityInfo, Long userId) {
        Reservation reservation = reservationService.createReservation(
                seatAvailabilityInfo.seatId(),
                seatAvailabilityInfo.price(),
                userId
        );

        concertService.occupySeat(seatAvailabilityInfo.seatId());

        return reservation;
    }
}
