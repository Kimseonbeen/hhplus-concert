package kr.hhplus.be.server.balance.domain.repository;

import kr.hhplus.be.server.balance.domain.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    Optional<Balance> findByUserId(Long userId);
}
