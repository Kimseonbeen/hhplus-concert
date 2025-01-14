package kr.hhplus.be.server.balance.domain.exception;

import lombok.Getter;

@Getter
public enum BalanceErrorCode {

    BALANCE_NOT_FOUND(400, "BALANCE_1001", "잔액 정보를 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(400, "BALANCE_1002", "잔액이 부족합니다."),
    INVALID_AMOUNT(400, "BALANCE_1003", "충전 금액을 확인해주세요.");

    private final int status;
    private final String code;
    private final String msg;


    BalanceErrorCode(int status, String code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }
}
