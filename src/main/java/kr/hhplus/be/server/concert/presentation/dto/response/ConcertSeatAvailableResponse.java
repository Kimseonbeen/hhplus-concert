package kr.hhplus.be.server.concert.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "예약 가능 좌석 요청")
public record ConcertSeatAvailableResponse(
        LocalDateTime date,
        List<Integer> availableSeats
) {

}
