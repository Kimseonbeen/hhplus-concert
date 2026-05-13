package kr.hhplus.be.server.concert.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "예약 가능 날짜 응답")
public record ConcertScheduleResponse(
        @Schema(description = "콘서트 스케줄 ID", example = "1")
        Long concertScheduleId,
        @Schema(description = "콘서트 날짜", example = "2026-06-01T18:00:00")
        LocalDateTime concertDate
) {

    public static ConcertScheduleResponse from(ConcertSchedule schedule) {
        return ConcertScheduleResponse.builder()
                .concertScheduleId(schedule.getId())
                .concertDate(schedule.getConcertDate())
                .build();
    }
}
