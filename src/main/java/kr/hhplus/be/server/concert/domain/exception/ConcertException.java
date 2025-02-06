package kr.hhplus.be.server.concert.domain.exception;

import kr.hhplus.be.server.common.exception.CustomException;

public class ConcertException extends CustomException {

    public ConcertException(ConcertErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg(), errorCode.getLogLevel());
    }
}
