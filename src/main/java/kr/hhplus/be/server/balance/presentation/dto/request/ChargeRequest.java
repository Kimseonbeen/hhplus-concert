package kr.hhplus.be.server.balance.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "충전 요청")
public record ChargeRequest(
        @Schema(description = "충전 금액", example = "1000")
        Long amount
) {}