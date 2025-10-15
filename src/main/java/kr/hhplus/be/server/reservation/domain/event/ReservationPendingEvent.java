package kr.hhplus.be.server.reservation.domain.event;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor  // 기본 생성자 자동 생성
@Getter
public class ReservationPendingEvent {
    private Long reservationId;
    private Long userId;
    private Long amount;
    private String token;
}
