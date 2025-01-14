package kr.hhplus.be.server.balance.domain.exception;

import kr.hhplus.be.server.common.exception.CustomException;

public class BalanceError extends CustomException {

    public BalanceError(int status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public BalanceError(BalanceErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg());
    }
}
