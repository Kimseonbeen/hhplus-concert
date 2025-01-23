package kr.hhplus.be.server.reservation.application.dto;

import lombok.Builder;

@Builder
public record ReservationCommand(
        Long userId,
        Long scheduleId,
        Long seatId
) {

    // 팩토리 메서드
    public static ReservationCommand of(String token, Long userId, Long scheduleId, Long seatId) {
        return ReservationCommand.builder()
                .userId(userId)
                .scheduleId(scheduleId)
                .seatId(seatId)
                .build();
    }
}
