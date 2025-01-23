package kr.hhplus.be.server.common.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;

@Getter
public class CustomException extends RuntimeException{
    private final int status;
    private final String errorCode;
    private final String message;
    private final LogLevel logLevel;

    public CustomException(int status, String errorCode, String message, LogLevel logLevel) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.logLevel = logLevel;
    }
}
