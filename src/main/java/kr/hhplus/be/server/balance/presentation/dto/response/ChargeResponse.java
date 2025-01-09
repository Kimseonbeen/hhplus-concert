package kr.hhplus.be.server.balance.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.balance.domain.Balance;
import lombok.Builder;

@Builder
@Schema(description = "충전 응답")
public record ChargeResponse(
        Long userId,
        @Schema(description = "충전 후 잔액", example = "2000")
        int amount
) {
    public static ChargeResponse from(Balance balance) {
        return new ChargeResponse(
                balance.getUserId(),
                balance.getAmount()
        );
    }
}
