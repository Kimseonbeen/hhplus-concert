package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.balance.domain.exception.BalanceError;
import kr.hhplus.be.server.balance.domain.model.Balance;
import kr.hhplus.be.server.balance.domain.model.BalanceHistory;
import kr.hhplus.be.server.balance.domain.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.balance.domain.repository.BalanceRepository;
import kr.hhplus.be.server.balance.domain.service.BalanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceHistoryRepository balanceHistoryRepository;

    @InjectMocks
    private BalanceService balanceService;

    @Test
    @DisplayName("잔액을 성공적으로 차감한다")
    void decrease_Success() {
        // given
        Long userId = 1L;
        Long amount = 50000L;
        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .amount(100000L)
                .build();

        given(balanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when
        balanceService.decrease(userId, amount);

        // then
        assertEquals(50000, balance.getAmount());
        verify(balanceHistoryRepository).save(any(BalanceHistory.class));
    }

    @Test
    @DisplayName("잔액이 부족하면 예외가 발생한다")
    void decrease_InsufficientBalance() {
        // given
        Long userId = 1L;
        Long amount = 150000L;
        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .amount(100000L)
                .build();

        given(balanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when & then
        assertThrows(BalanceError.class,
                () -> balanceService.decrease(userId, amount));
        verify(balanceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("잔액이 성공적으로 충전된다")
    void increase_Success() {
        // given
        Long userId = 1L;
        Long originalAmount = 50000L;
        Long chargeAmount = 100000L;

        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .amount(originalAmount)
                .build();

        given(balanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when
        balanceService.increase(userId, chargeAmount);

        // then
        assertEquals(originalAmount + chargeAmount, balance.getAmount());
        verify(balanceHistoryRepository).save(any(BalanceHistory.class));
    }

    @Test
    @DisplayName("충전 금액이 0 이하면 예외가 발생한다")
    void increase_InvalidAmount() {
        // given
        Long userId = 1L;
        Long invalidAmount = -10000L;

        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .amount(50000L)
                .build();

        given(balanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when & then
        assertThrows(BalanceError.class,
                () -> balanceService.increase(userId, invalidAmount));
        verify(balanceHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자의 잔액을 조회한다")
    void getBalance_Success() {
        // given
        Long userId = 1L;
        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .amount(50000L)
                .build();

        given(balanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when
        Balance result = balanceService.getBalance(userId);

        // then
        assertNotNull(result);
        assertEquals(50000, result.getAmount());
    }
}
