package kr.hhplus.be.server.reservation.application.dto;

import lombok.Builder;

@Builder
public record PaymentCommand(
        Long reservationId,
        Long userId,
        String token
) {
}
