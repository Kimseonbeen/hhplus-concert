package kr.hhplus.be.server.balance.domain.model;

import kr.hhplus.be.server.balance.domain.exception.BalanceError;
import kr.hhplus.be.server.balance.domain.exception.BalanceErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BalanceTest {

    @Test
    @DisplayName("3000원에서 1000원 차감시 잔액이 2000원이된다.")
    void decrease_Success() {
        // given
        Balance balance = Balance.builder()
                .id(1L)
                .userId(1L)
                .amount(3000L)
                .build();

        // when
        balance.decrease(1000L);

        // then
        assertEquals(balance.getAmount(), 2000L);
    }

    @Test
    @DisplayName("3000원에서 4000원 차감시 BalanceErrorCode 에러를 반환한다.")
    void decrease_ReturnBalanceErrorCode() {
        // given
        Balance balance = Balance.builder()
                .id(1L)
                .userId(1L)
                .amount(3000L)
                .build();

        // when
        BalanceError balanceError = assertThrows(BalanceError.class, () -> {
            balance.decrease(4000L);
        });

        // then
        assertEquals(balanceError.getMessage(), BalanceErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    @Test
    @DisplayName("3000원에서 1000원 추가시 잔액이 4000원이된다.")
    void increase_Success() {
        // given
        Balance balance = Balance.builder()
                .id(1L)
                .userId(1L)
                .amount(3000L)
                .build();

        // when
        balance.increase(1000L);

        // then
        assertEquals(balance.getAmount(), 4000L);
    }

    @Test
    @DisplayName("추가 금액이 0원 이하 일 시 BalanceErrorCode 에러를 반환한다.")
    void increase_ReturnBalanceErrorCode() {
        // given
        Balance balance = Balance.builder()
                .id(1L)
                .userId(1L)
                .amount(3000L)
                .build();

        // when
        BalanceError balanceError =  assertThrows(BalanceError.class, () -> {
            balance.increase(0L);
        });

        // then
        assertEquals(balanceError.getMessage(), BalanceErrorCode.INVALID_AMOUNT.getMsg());
    }
}