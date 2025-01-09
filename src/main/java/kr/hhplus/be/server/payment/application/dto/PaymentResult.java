package kr.hhplus.be.server.payment.application.dto;

import kr.hhplus.be.server.payment.domain.Payment;

import java.time.LocalDateTime;

public record PaymentResult(
        Long paymentId,
        Long userId,
        Long reservationId,
        Integer amount,
        LocalDateTime createdAt
) {

    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getUserId(),
                payment.getReservationId(),
                payment.getAmount(),
                payment.getCreatedAt()
        );
    }
}
