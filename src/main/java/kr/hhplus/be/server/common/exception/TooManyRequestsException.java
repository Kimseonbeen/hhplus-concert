package kr.hhplus.be.server.common.exception;

import org.springframework.boot.logging.LogLevel;

public class TooManyRequestsException extends CustomException {

    public TooManyRequestsException(String message) {
        super(429, "COMMON_4291", message, LogLevel.WARN);
    }
}
