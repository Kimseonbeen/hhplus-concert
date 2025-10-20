package kr.hhplus.be.server.concert.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "예약 가능 좌석 요청")
public record ConcertSeatAvailableResponse(
        LocalDateTime date,
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