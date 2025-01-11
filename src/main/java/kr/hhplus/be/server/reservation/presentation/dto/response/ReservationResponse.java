package kr.hhplus.be.server.reservation.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "예약 응답")
public record ReservationResponse(
        @Schema(description = "응답 메시지", example = "좌석 예약에 성공했습니다.")
        String message,

        @Schema(description = "예약 ID", example = "1")
        Long reservationId
) {}