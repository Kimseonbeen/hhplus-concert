package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert.domain.exception.ConcertError;
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

    public void isAvailable() {
        if (concertDate.isBefore(LocalDateTime.now())) {
            throw new ConcertError(ConcertErrorCode.SEAT_ALREADY_OCCUPIED);
        }
    }
}
