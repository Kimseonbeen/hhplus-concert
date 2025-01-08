package kr.hhplus.be.server.concert.domain.exception;

import kr.hhplus.be.server.common.exception.CustomException;

public class ConcertError extends CustomException {

    public ConcertError(int status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public ConcertError(ConcertErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg());
    }
}
