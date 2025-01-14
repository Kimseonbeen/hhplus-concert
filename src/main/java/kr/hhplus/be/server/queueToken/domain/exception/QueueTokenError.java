package kr.hhplus.be.server.queueToken.domain.exception;

import kr.hhplus.be.server.common.exception.CustomException;

public class QueueTokenError extends CustomException {

    public QueueTokenError(int status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public QueueTokenError(QueueTokenErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg());
    }
}
