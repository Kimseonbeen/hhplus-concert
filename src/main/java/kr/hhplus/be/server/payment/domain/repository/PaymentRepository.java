package kr.hhplus.be.server.payment.domain.repository;

import kr.hhplus.be.server.payment.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
