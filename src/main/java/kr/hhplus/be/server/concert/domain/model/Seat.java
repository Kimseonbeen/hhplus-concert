package kr.hhplus.be.server.concert.domain.model;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.exception.ConcertError;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "concert_schedule_id", nullable = false)
    private Long concertScheduleId;

    @Column(name = "seat_num", nullable = false)
    private Integer seatNum;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    public void isAvailable() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new ConcertError(ConcertErrorCode.SEAT_ALREADY_OCCUPIED);
        }
    }

    public void occupy() {
        this.status = SeatStatus.TEMPORARY;
    }

    public void reserved() {
        this.status = SeatStatus.RESERVED;
    }
}
