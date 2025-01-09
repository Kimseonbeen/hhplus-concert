package kr.hhplus.be.server.balance.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.balance.domain.Balance;
import lombok.Builder;

@Builder
@Schema(description = "잔액 응답")
public record BalanceResponse(
        Long userId,
        int amount
) {
        public static BalanceResponse from(Balance balance) {
                return new BalanceResponse(
                        balance.getUserId(),
                        balance.getAmount());
        }
}