package kr.hhplus.be.server.reservation.domain;

public enum ReservationStatus {
    PENDING_PAYMENT,  // 결제 대기
    CONFIRMED,       // 예약 확정
    CANCELLED        // 취소됨
}
