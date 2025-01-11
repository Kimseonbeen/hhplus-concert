package kr.hhplus.be.server.balance.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "충전 응답")
public record ChargeResponse(
        @Schema(description = "응답 메시지", example = "잔액 충전이 완료되었습니다.")
        String message,

        @Schema(description = "충전 후 잔액", example = "2000")
        String balance
) {}
