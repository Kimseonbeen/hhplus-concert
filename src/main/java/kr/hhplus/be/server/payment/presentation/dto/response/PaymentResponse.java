package kr.hhplus.be.server.payment.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.concert.presentation.dto.response.SeatResponse;
import kr.hhplus.be.server.payment.application.dto.PaymentResult;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.presentation.dto.response.ReservationResponse;
import lombok.Builder;

@Builder
@Schema(description = "결제 응답")
public record PaymentResponse(
        Long paymentId,
        Long userId,
        Long reservationId,
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