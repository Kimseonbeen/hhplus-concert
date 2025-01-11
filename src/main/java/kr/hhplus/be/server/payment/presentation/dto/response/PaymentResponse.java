package kr.hhplus.be.server.payment.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "결제 응답")
public record PaymentResponse(
        @Schema(description = "응답 메시지", example = "결제에 성공했습니다.")
        String message,
        @Schema(description = "결제 ID", example = "1")
        Long paymentId

) {}