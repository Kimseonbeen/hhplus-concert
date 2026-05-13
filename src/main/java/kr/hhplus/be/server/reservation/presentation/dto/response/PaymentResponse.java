package kr.hhplus.be.server.reservation.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.reservation.application.dto.PaymentResult;
import lombok.Builder;

@Builder
@Schema(description = "결제 응답")
public record PaymentResponse(
        @Schema(description = "결제 ID", example = "1")
        Long paymentId,
        @Schema(description = "유저 ID", example = "1")
        Long userId,
        @Schema(description = "예약 ID", example = "1")
        Long reservationId,
        @Schema(description = "결제 금액", example = "50000")
        Long amount

) {
        public static PaymentResponse from(PaymentResult payment) {
                return PaymentResponse.builder()
                        .paymentId(payment.paymentId())
                        .amount(payment.amount())
                        .userId(payment.userId())
                        .reservationId(payment.reservationId())
                        .build();
        }

}