package kr.hhplus.be.server.concert.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "좌석 응답")
public record SeatResponse(
        @Schema(description = "좌석 ID", example = "1")
        Long seatId,
        @Schema(description = "좌석 번호", example = "15")
        Long seatNo,
        @Schema(description = "좌석 가격", example = "50000")
        Long seatPrice
) {

}
