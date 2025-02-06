package kr.hhplus.be.server.queueToken.domain.exception;

import kr.hhplus.be.server.common.exception.CustomException;

public class QueueTokenException extends CustomException {

    public QueueTokenException(QueueTokenErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg(), errorCode.getLogLevel());
    }
}
