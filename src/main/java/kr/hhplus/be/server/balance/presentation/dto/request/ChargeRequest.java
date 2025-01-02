package kr.hhplus.be.server.balance.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "충전 요청")
public record ChargeRequest(
        @Schema(description = "유저 아이디", example = "user123")
        String userId,

        @Schema(description = "충전 금액", example = "1000.0")
        String amount
) {}