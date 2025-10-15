package kr.hhplus.be.server.reservation.domain.event;

import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResultEvent {
    private Long reservationId;
    private Long userId;
    private Long amount;
    private ReservationStatus status;

    // 토큰이 필요한 경우 추가
    private String token;

    // 토큰 없는 생성자 오버로딩
    public ReservationResultEvent(Long reservationId, Long userId, Long amount, ReservationStatus status) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.token = null;
    }
}
