package kr.hhplus.be.server.concert.presentation.dto.response;

import lombok.Builder;

@Builder
public record SeatResponse(
        Long seatId,
        Long seatNo,
        Long seatPrice
) {

}
