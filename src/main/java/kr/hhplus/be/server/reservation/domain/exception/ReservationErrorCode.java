package kr.hhplus.be.server.reservation.domain.exception;

import lombok.Getter;
import org.springframework.boot.logging.LogLevel;

@Getter
public enum ReservationErrorCode {

    RESERVATION_NOT_FOUND(400, "RESERVATION_1001", "예약정보를 찾을 수 없습니다.", LogLevel.WARN);

    private final int status;
    private final String code;
    private final String msg;
    private final LogLevel logLevel;


    ReservationErrorCode(int status, String code, String msg, LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.msg = msg;
        this.logLevel = logLevel;
    }
}
