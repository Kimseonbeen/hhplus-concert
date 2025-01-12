package kr.hhplus.be.server.balance.domain.repository;

import kr.hhplus.be.server.balance.domain.model.BalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, Long> {
}
