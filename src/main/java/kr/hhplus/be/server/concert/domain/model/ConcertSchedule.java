package kr.hhplus.be.server.concert.domain.model;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ConcertSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long concertId;

    @Column(nullable = false)
    private LocalDateTime concertDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConcertScheduleStatus status;

    public void checkIsAvailable() {
        if (this.concertDate.isBefore(LocalDateTime.now())) {
            throw new ConcertException(ConcertErrorCode.CONCERT_DATE_EXPIRED);
        }
    }
}
