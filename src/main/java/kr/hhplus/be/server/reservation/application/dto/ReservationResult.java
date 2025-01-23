package kr.hhplus.be.server.reservation.application.dto;

import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationResult(
        Long userId,
        Long seatId,
        Long price

) {
    // 팩토리 메서드
    public static ReservationResult of(Long userId, Long seatId, Long price) {
        return new ReservationResult(userId, seatId, price);
    }


}