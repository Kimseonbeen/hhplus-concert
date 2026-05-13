package kr.hhplus.be.server.reservation.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.reservation.application.dto.PaymentCommand;

@Schema(description = "결제 요청")
public record PaymentRequest(
        @Schema(description = "예약 ID", example = "1")
        Long reservationId,
        @Schema(description = "유저 ID", example = "1")
        Long userId
) {

    public PaymentCommand toCommand() {
        return PaymentCommand.builder()
                .reservationId(this.reservationId)
                .userId(this.userId)
                .build();
    }
}
