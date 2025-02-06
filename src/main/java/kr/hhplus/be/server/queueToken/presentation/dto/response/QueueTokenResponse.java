package kr.hhplus.be.server.queueToken.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "대기열 토큰 응답")
public record QueueTokenResponse(
        String token,
        QueueTokenStatus status,
        long num,
        LocalDateTime expiredAt
) {

    public static QueueTokenResponse from(QueueToken queueToken) {
        return QueueTokenResponse.builder()
                .token(queueToken.getToken())
                .status(queueToken.getStatus())
                //.num(queueToken.getWaitingNumber())
                .expiredAt(queueToken.getExpiredAt())
                .build();
   }

    public static QueueTokenResponse of(QueueToken queueToken) {
        return QueueTokenResponse.builder()
                .token(queueToken.getToken())
                .status(queueToken.getStatus())
                .num(queueToken.getPosition())
                .expiredAt(queueToken.getExpiredAt())
                .build();
    }


}
