package kr.hhplus.be.server.reservation.domain.model;

public enum ReservationStatus {
    PENDING_PAYMENT,  // 결제 대기
    CONFIRMED,        // 예약 확정
    CANCELLED,        // 취소됨
    EXPIRED           // 만료됨 (5분 내 결제 미완료)
}
