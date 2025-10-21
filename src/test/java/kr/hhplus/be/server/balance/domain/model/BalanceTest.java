package kr.hhplus.be.server.balance.domain.model;

import kr.hhplus.be.server.balance.domain.exception.BalanceError;
import kr.hhplus.be.server.balance.domain.exception.BalanceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BalanceTest {

    Balance balance;
    final long INITIAL_AMOUNT = 3000L;
    final long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        balance = Balance.builder()
                .id(1L)
                .userId(USER_ID)
                .amount(INITIAL_AMOUNT)
                .build();
    }

    @Test
    @DisplayName("3000원에서 1000원 차감시 잔액이 2000원이된다.")
    void decrease_Success() {
        // given
        long decreaseAmount = 1000L;

        // when
        balance.decrease(decreaseAmount);

        // then
        assertThat(balance.getAmount()).isEqualTo(INITIAL_AMOUNT - decreaseAmount);
    }

    @Test
    @DisplayName("3000원에서 4000원 차감시 BalanceErrorCode 에러를 반환한다.")
    void decrease_ThrowsInsufficientBalance_WhenAmountExceedsBalance() {
        // given
        long decreaseAmount = 4000L;

        // when
        BalanceError balanceError = assertThrows(BalanceError.class, () -> {
            balance.decrease(decreaseAmount);
        });

        // then
        assertThat(balanceError.getMessage()).isEqualTo(BalanceErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    @Test
    @DisplayName("3000원에서 1000원 증가시 잔액이 4000원이된다.")
    void increase_Success() {
        // given
        long increaseAmount = 1000L;

        // when
        balance.increase(increaseAmount);

        // then
        assertThat(balance.getAmount()).isEqualTo(INITIAL_AMOUNT + increaseAmount);
    }

    @Test
    @DisplayName("추가 금액이 0원 이하 일 시 BalanceErrorCode 에러를 반환한다.")
    void increase_ThrowsInvalidAmountError_WhenAmountIsZeroOrMinus() {
        // given
        long increaseAmount = 0L;

        // when
        BalanceError balanceError =  assertThrows(BalanceError.class, () -> {
            balance.increase(increaseAmount);
        });

        // then
        assertThat(balanceError.getMessage()).isEqualTo(BalanceErrorCode.INVALID_AMOUNT.getMsg());
    }
}