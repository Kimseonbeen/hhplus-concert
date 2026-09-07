package kr.hhplus.be.server.concert.domain.model;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // JPA 낙관적 락(Optimistic Lock)용 버전 컬럼.
    // 두 트랜잭션이 동시에 같은 좌석을 조회 후 각각 update를 시도하면,
    // 먼저 커밋한 쪽만 성공하고 나머지는 버전 불일치로 OptimisticLockException 발생.
    // 좌석 예약은 충돌 빈도가 낮고 "실패 시 재조회 유도"가 자연스러운 흐름이라
    // 매 요청마다 락을 거는 비관적 락 대신 낙관적 락을 선택했다.
    @Version
    private Long version;

    @Column(name = "concert_schedule_id", nullable = false)
    private Long concertScheduleId;

    @Column(name = "seat_num", nullable = false)
    private Integer seatNum;

    @Column(nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    public void reserved() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new ConcertException(ConcertErrorCode.SEAT_ALREADY_OCCUPIED);
        }
        this.status = SeatStatus.RESERVED;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }
}
