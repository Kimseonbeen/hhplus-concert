package kr.hhplus.be.server.payment.domain;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SeatRepository seatRepository;

    public Payment processPayment(Long reservationId, Long userId, int amount) {

        Payment payment = Payment.createPayment(reservationId, userId, amount);

        return paymentRepository.save(payment);
    }
}
