package kr.hhplus.be.server.queueToken.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대기열 토큰 발급 요청")
public record QueueTokenRequest(
        @Schema(description = "유저 ID", example = "1")
        long userId
) {
}
