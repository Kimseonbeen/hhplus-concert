package kr.hhplus.be.server.payment.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 요청")
public record PaymentRequest(
        long reservationId
) {
}
