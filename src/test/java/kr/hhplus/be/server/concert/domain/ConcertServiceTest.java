package kr.hhplus.be.server.concert.domain;

import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.model.ConcertScheduleStatus;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.concert.domain.service.ConcertService;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock
    private ConcertScheduleRepository concertScheduleRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private ConcertService concertService;

    @Test
    @DisplayName("콘서트 1번 아이디로 조회시 예약가능한 공연 일정이 반환된다")
    void getConcertSchedules_ReturnAllSchedules() {
        // given
        Long concertId = 1L;
        LocalDateTime concertDate = LocalDateTime.now();

        List<ConcertSchedule> allConcerts = List.of(
                ConcertSchedule.builder()   // 예약가능
                        .id(concertId)
                        .concertDate(concertDate.plusDays(1))
                        .status(ConcertScheduleStatus.AVAILABLE)
                        .build(),
                ConcertSchedule.builder()   // SOLDOUT 상태라 예약 불가능
                        .id(concertId)
                        .concertDate(concertDate.plusDays(1))
                        .status(ConcertScheduleStatus.SOLDOUT)
                        .build(),
                ConcertSchedule.builder()   // 예약 가능 기간이 지나 예약 불가능
                        .id(concertId)
                        .concertDate(concertDate.minusDays(2))
                        .status(ConcertScheduleStatus.AVAILABLE)
                        .build()
        );

        given(concertScheduleRepository.findByConcertId(concertId)).willReturn(allConcerts);

        // when
        List<ConcertScheduleResponse> result = concertService.getConcertSchedules(concertId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).concertScheduleId()).isEqualTo(concertId);
        verify(concertScheduleRepository).findByConcertId(concertId);
    }


    @Test
    @DisplayName("공연 좌석 목록 조회 시 AVAILABLE 상태의 좌석만 반환되고 RESERVED 상태의 좌석은 제외된다")
    void getAvailableSeats_WhenSeatsAvailable_ReturnOnlyAvailableSeats() {
        // given
        Long scheduleId = 1L;
        LocalDateTime concertDate = LocalDateTime.now().plusDays(1);

        ConcertSchedule schedule = ConcertSchedule.builder()
                .id(scheduleId)
                .concertDate(concertDate)
                .build();

        // AVAILABLE과 RESERVED, TEMPORARY 상태가 섞인 전체 좌석 목록
        List<Seat> allSeats = List.of(
                Seat.builder()
                        .seatNum(1)
                        .status(SeatStatus.AVAILABLE)
                        .build(),
                Seat.builder()
                        .seatNum(2)
                        .status(SeatStatus.AVAILABLE)
                        .build(),
                Seat.builder()
                        .seatNum(3)
                        .status(SeatStatus.RESERVED)  // 예약된 좌석
                        .build()
        );

        // AVAILABLE 상태의 좌석만 필터링된 목록
        List<Seat> availableSeats = allSeats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .collect(Collectors.toList());

        given(concertScheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
        given(seatRepository.findByConcertScheduleIdAndStatus(scheduleId, SeatStatus.AVAILABLE))
                .willReturn(availableSeats);

        // when
        ConcertSeatAvailableResponse result = concertService.getAvailableSeats(scheduleId);

        // then
        assertThat(result.date()).isEqualTo(concertDate);
        assertThat(result.availableSeats()).hasSize(2);  // AVAILABLE 좌석 2개만 포함
        assertThat(result.availableSeats()).containsExactly(1, 2);  // (3, 4번) 좌석 제외
        assertThat(result.availableSeats()).doesNotContain(3);  // RESERVED 좌석 미포함 확인

        verify(concertScheduleRepository).findById(scheduleId);
        verify(seatRepository).findByConcertScheduleIdAndStatus(scheduleId, SeatStatus.AVAILABLE);
    }
}