package kr.hhplus.be.server.queueToken.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "대기열 토큰 응답")
public record QueueTokenResponse(
        @Schema(description = "대기열 토큰", example = "uuid-1234-abcd")
        String token,
        @Schema(description = "토큰 상태 (WAITING/ACTIVE)", example = "WAITING")
        QueueTokenStatus status,
        @Schema(description = "대기 순번", example = "5")
        Long num,
        @Schema(description = "토큰 만료 시간", example = "2026-06-01T18:10:00")
        LocalDateTime expiredAt
) {

    public static QueueTokenResponse from(QueueToken queueToken) {
        return QueueTokenResponse.builder()
                .token(queueToken.getToken())
                .status(queueToken.getStatus())
                .num(queueToken.getPosition() != null ? queueToken.getPosition() : 0L)
                .expiredAt(queueToken.getExpiredAt())
                .build();
    }
}
