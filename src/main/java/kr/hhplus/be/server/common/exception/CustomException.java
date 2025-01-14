package kr.hhplus.be.server.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{
    private final int status;
    private final String errorCode;
    private final String message;

    public CustomException(int status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }
}
