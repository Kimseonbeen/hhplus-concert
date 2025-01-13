package kr.hhplus.be.server.balance.domain.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.balance.domain.exception.BalanceError;
import kr.hhplus.be.server.balance.domain.exception.BalanceErrorCode;
import kr.hhplus.be.server.balance.domain.model.Balance;
import kr.hhplus.be.server.balance.domain.model.BalanceHistory;
import kr.hhplus.be.server.balance.domain.model.BalanceHistoryType;
import kr.hhplus.be.server.balance.domain.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.domain.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    public void decrease(Long userId, Long amount) {
        // 1. 잔액 조회
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceError(BalanceErrorCode.BALANCE_NOT_FOUND));

        // 2. 잔액 감소
        balance.decrease(amount);  // 잔액이 부족하면 여기서 예외 발생

        // 3. 잔액 변경 이력 저장
        BalanceHistory history = BalanceHistory.builder()
                .balanceId(balance.getId())
                .amount(amount)          // 양수 그대로 저장
                .type(BalanceHistoryType.DECREASE)  // 감소로 명시
                .build();
        balanceHistoryRepository.save(history);
    }

    @Transactional
    public void increase(Long userId, Long amount) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceError(BalanceErrorCode.BALANCE_NOT_FOUND));

        balance.increase(amount);

        BalanceHistory history = BalanceHistory.builder()
                .balanceId(balance.getId())
                .amount(amount)
                .type(BalanceHistoryType.INCREASE)
                .build();

        balanceHistoryRepository.save(history);
    }

    public Balance getBalance(Long userId) {
        return balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceError(BalanceErrorCode.BALANCE_NOT_FOUND));
    }

}
