package kr.hhplus.be.server.queueToken.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.queueToken.domain.QueueTokenStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Schema(description = "대기열 토큰 응답")
public record QueueTokenResponse(
        String token,
        QueueTokenStatus status,
        int num,
        LocalDateTime expiredAt
) {
}
