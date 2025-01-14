package kr.hhplus.be.server.reservation.application.dto;

import lombok.Builder;

public record ReservationCommand(
        String token,
        Long userId,
        Long scheduleId,
        Long seatId
) {
    @Builder
    public ReservationCommand {
        // 유효성 검증
        //validate(token, userId, scheduleId, seatId);
    }

    private void validate(String token, Long userId, Long scheduleId, Long seatId) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("토큰은 필수입니다.");
        }

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        if (scheduleId == null || scheduleId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 스케줄 ID입니다.");
        }

        if (seatId == null || seatId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 좌석 ID입니다.");
        }
    }

    // 팩토리 메서드
    public static ReservationCommand of(String token, Long userId, Long scheduleId, Long seatId) {
        return ReservationCommand.builder()
                .token(token)
                .userId(userId)
                .scheduleId(scheduleId)
                .seatId(seatId)
                .build();
    }
}
