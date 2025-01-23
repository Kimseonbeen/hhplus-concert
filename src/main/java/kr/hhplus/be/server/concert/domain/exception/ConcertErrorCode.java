package kr.hhplus.be.server.concert.domain.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;

@Getter
public enum ConcertErrorCode {

    CONCERT_NOT_FOUND(400, "CONCERT_1001", "해당 콘서트 정보가 없습니다.", LogLevel.WARN),
    SEAT_NOT_FOUND(400, "CONCERT_1002", "해당 좌석 정보가 없습니다.", LogLevel.WARN),
    CONCERT_DATE_EXPIRED(400, "CONCERT_1003", "이미 종료된 콘서트입니다.", LogLevel.WARN),

    SEAT_ALREADY_OCCUPIED(400, "CONCERT_2001", "현재 예약할 수 없는 좌석입니다.", LogLevel.WARN);

    private final int status;
    private final String code;
    private final String msg;
    private final LogLevel logLevel;

    ConcertErrorCode(int status, String code, String msg, LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.msg = msg;
        this.logLevel = logLevel;
    }
}
