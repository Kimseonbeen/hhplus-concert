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
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    // 잔액 충전/차감은 좌석 예약과 달리 "동시에 같은 유저가 여러 요청을 보내는" 충돌이
    // 실제로 자주 발생할 수 있고, 실패 후 재시도를 유저에게 맡기기 부적절한 도메인(돈)이라
    // 낙관적 락 대신 Redisson 분산 락을 사용해 같은 userId에 대한 요청을 직렬화한다.
    // key에 userId를 포함해 "유저 단위"로만 락을 걸고, 다른 유저의 요청은 대기 없이 처리되게 한다.
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
