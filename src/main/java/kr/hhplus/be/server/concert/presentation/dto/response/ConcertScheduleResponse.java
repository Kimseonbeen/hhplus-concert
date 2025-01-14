package kr.hhplus.be.server.concert.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "예약 가능 날짜 요청")
public record ConcertScheduleResponse(
        long concertScheduleId,
        LocalDateTime concertDate
) {
}
