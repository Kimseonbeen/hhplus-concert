package kr.hhplus.be.server.reservation.presentation.scheduler;

import kr.hhplus.be.server.reservation.application.ReservationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationFacade reservationFacade;

    @Scheduled(fixedDelay = 60 * 1000) // 1분마다 실행
    public void expireOverdueReservations() {
        log.info("[ReservationScheduler] 만료 예약 처리 시작");
        try {
            reservationFacade.expireOverdueReservations();
            log.info("[ReservationScheduler] 만료 예약 처리 완료");
        } catch (Exception e) {
            log.error("[ReservationScheduler] 만료 예약 처리 중 오류 발생", e);
        }
    }
}
