package kr.hhplus.be.server.payment.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.payment.application.dto.PaymentCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;

@Schema(description = "결제 요청")
public record PaymentRequest(
        Long reservationId,
        Long userId
) {

    public PaymentCommand toCommand() {
        return PaymentCommand.builder()
                .reservationId(this.reservationId)
                .userId(this.userId)
                .build();
    }
}
