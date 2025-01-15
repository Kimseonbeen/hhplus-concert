package kr.hhplus.be.server.reservation.domain.exception;

import kr.hhplus.be.server.common.exception.CustomException;

public class ReservationError extends CustomException {

    public ReservationError(ReservationErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getCode(), errorCode.getMsg(), errorCode.getLogLevel());
    }
}
