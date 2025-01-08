package kr.hhplus.be.server.concert.domain.exception;

import lombok.Getter;

@Getter
public enum ConcertErrorCode {

    CONCERT_NOT_FOUND(400, "CONCERT_1001", "해당 콘서트 정보가 없습니다.");

    private final int status;
    private final String code;
    private final String msg;


    ConcertErrorCode(int status, String code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }
}
