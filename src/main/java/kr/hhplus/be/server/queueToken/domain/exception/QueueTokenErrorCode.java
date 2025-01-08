package kr.hhplus.be.server.queueToken.domain.exception;

import lombok.Getter;

@Getter
public enum QueueTokenErrorCode {

    QUEUE_TOKEN_NOT_FOUND(400, "QUEUE_TOKEN_1001", "해당 대기열 정보가 없습니다.");

    private final int status;
    private final String code;
    private final String msg;


    QueueTokenErrorCode(int status, String code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }
}
