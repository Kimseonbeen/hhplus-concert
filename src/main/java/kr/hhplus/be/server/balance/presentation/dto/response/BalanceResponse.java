package kr.hhplus.be.server.balance.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "잔액 응답")
public record BalanceResponse(
        @Schema(description = "현재 잔액", example = "1000")
        String balance,
        long userId
) {}