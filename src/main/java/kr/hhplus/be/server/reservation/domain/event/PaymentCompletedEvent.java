package kr.hhplus.be.server.reservation.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PaymentCompletedEvent {
    private final Long paymentId;
}
