package kr.hhplus.be.server.balance.domain.service;

import kr.hhplus.be.server.balance.domain.exception.BalanceException;
import kr.hhplus.be.server.balance.domain.exception.BalanceErrorCode;
import kr.hhplus.be.server.balance.domain.model.Balance;
import kr.hhplus.be.server.balance.domain.model.BalanceHistory;
import kr.hhplus.be.server.balance.domain.model.BalanceHistoryType;
import kr.hhplus.be.server.balance.domain.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.domain.repository.BalanceRepository;
import kr.hhplus.be.server.common.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;
    // TODO: 이벤트 발행 용도로 사용 예정
    private final KafkaTemplate<String, String> kafkaTemplate;

    @DistributedLock(key = "'point :' + #userId")
    public void decrease(Long userId, Long amount) {
        // 1. 잔액 조회
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceException(BalanceErrorCode.BALANCE_NOT_FOUND));

        // 2. 잔액 감소
        balance.decrease(amount);  // 잔액이 부족하면 여기서 예외 발생

        // 3. 잔액 변경 이력 저장
        BalanceHistory history = BalanceHistory.createHistory(amount, balance, BalanceHistoryType.DECREASE);

        balanceHistoryRepository.save(history);
    }

    @DistributedLock(key = "'point :' + #userId")
    public void increase(Long userId, Long amount) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceException(BalanceErrorCode.BALANCE_NOT_FOUND));

        balance.increase(amount);

        BalanceHistory history = BalanceHistory.createHistory(amount, balance, BalanceHistoryType.INCREASE);

        balanceHistoryRepository.save(history);
    }

    public Balance getBalance(Long userId) {
        return balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new BalanceException(BalanceErrorCode.BALANCE_NOT_FOUND));
    }
}
