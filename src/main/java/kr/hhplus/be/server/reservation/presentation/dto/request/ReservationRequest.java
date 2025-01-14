package kr.hhplus.be.server.reservation.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;

import java.time.LocalDateTime;

@Schema(description = "좌석 예약 요청")
public record ReservationRequest(
        long scheduleId,
        long seatId,
        long userId,
        LocalDateTime date,
        int seatNum
) {
    public ReservationCommand toCommand() {
        return ReservationCommand.builder()
                .scheduleId(this.scheduleId)
                .seatId(this.seatId)
                .userId(this.userId)
                .build();
    }
}
