package kr.hhplus.be.server.reservation.domain.exception;

import lombok.Getter;

@Getter
public enum ReservationErrorCode {

    QUEUE_TOKEN_NOT_FOUND(400, "QUEUE_TOKEN_1001", "해당 대기열 토큰 정보가 없습니다."),
    QUEUE_TOKEN_EXPIRED(400, "QUEUE_TOKEN_1002", "해당 대기열 토큰이 만료되었습니다."),
    QUEUE_TOKEN_NOT_ACTIVE(400, "QUEUE_TOKEN_1003", "해당 대기열 토큰이 활성화되어있지 않습니다.");

    private final int status;
    private final String code;
    private final String msg;


    ReservationErrorCode(int status, String code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }
}
