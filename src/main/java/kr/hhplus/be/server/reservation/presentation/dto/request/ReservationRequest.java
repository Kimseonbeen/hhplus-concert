package kr.hhplus.be.server.reservation.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;

@Schema(description = "좌석 예약 요청")
public record ReservationRequest(
        @Schema(description = "콘서트 스케줄 ID", example = "1")
        long scheduleId,
        @Schema(description = "좌석 ID", example = "1")
        long seatId,
        @Schema(description = "유저 ID", example = "1")
        long userId
) {
    public ReservationCommand toCommand() {
        return ReservationCommand.builder()
                .scheduleId(this.scheduleId)
                .seatId(this.seatId)
                .userId(this.userId)
                .build();
    }
}
