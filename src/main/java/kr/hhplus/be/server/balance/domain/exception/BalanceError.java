package kr.hhplus.be.server.balance.domain.exception;

import kr.hhplus.be.server.common.exception.CustomException;

public class BalanceError extends CustomException {

    public BalanceError(BalanceErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg(), errorCode.getLogLevel());
    }
}
