package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.balance.domain.exception.BalanceError;
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
        Integer amount = 50000;
        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .amount(100000)
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
        Integer amount = 150000;
        Balance balance = Balance.builder()
                .id(1L)
                .userId(userId)
                .amount(100000)
                .build();

        given(balanceRepository.findByUserId(userId))
                .willReturn(Optional.of(balance));

        // when & then
        assertThrows(BalanceError.class,
                () -> balanceService.decrease(userId, amount));
        verify(balanceHistoryRepository, never()).save(any());
    }
}
