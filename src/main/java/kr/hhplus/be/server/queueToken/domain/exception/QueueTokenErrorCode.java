package kr.hhplus.be.server.queueToken.domain.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;

@Getter
public enum QueueTokenErrorCode {

    QUEUE_TOKEN_NOT_FOUND(400, "QUEUE_TOKEN_1001", "해당 대기열 토큰 정보가 없습니다.", LogLevel.WARN),
    QUEUE_TOKEN_EXPIRED(400, "QUEUE_TOKEN_1002", "해당 대기열 토큰이 만료되었습니다.", LogLevel.WARN),
    QUEUE_TOKEN_NOT_ACTIVE(400, "QUEUE_TOKEN_1003", "해당 대기열 토큰이 활성화되어있지 않습니다.", LogLevel.WARN);

    private final int status;
    private final String code;
    private final String msg;
    private final LogLevel logLevel;


    QueueTokenErrorCode(int status, String code, String msg, LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.msg = msg;
        this.logLevel = logLevel;
    }
}
