package kr.hhplus.be.server.reservation.application.dto;

import lombok.Builder;

@Builder
public record PaymentCommand(
        Long reservationId,
        Long userId,
        Long amount,
        String token
) {
}
