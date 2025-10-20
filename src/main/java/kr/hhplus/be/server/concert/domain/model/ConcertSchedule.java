package kr.hhplus.be.server.concert.domain.model;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor
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
