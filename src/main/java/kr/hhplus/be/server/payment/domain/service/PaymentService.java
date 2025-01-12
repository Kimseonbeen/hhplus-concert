package kr.hhplus.be.server.payment.domain.service;

import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.repository.PaymentRepository;
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
