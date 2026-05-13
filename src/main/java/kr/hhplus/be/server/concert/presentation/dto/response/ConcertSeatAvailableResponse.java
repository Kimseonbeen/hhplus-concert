package kr.hhplus.be.server.concert.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "예약 가능 좌석 응답")
public record ConcertSeatAvailableResponse(
        @Schema(description = "콘서트 날짜", example = "2026-06-01T18:00:00")
        LocalDateTime date,
        @Schema(description = "예약 가능한 좌석 번호 목록", example = "[1, 2, 3, 4, 5]")
        List<Integer> availableSeats
) {

    public static ConcertSeatAvailableResponse from(
            ConcertSchedule concertSchedule,
            List<Integer> availableSeats
    ) {
        return ConcertSeatAvailableResponse.builder()
                .date(concertSchedule.getConcertDate())
                .availableSeats(availableSeats)
                .build();
    }
}
