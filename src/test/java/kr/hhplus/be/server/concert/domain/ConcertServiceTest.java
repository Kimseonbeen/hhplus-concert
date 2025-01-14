package kr.hhplus.be.server.concert.domain;

import kr.hhplus.be.server.concert.domain.exception.ConcertError;
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.repository.ConcertRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private ConcertService concertService;

    @Test
    @DisplayName("특정 ID로 조회시 해당 공연 일정이 반환된다")
    void getConcertSchedules_ReturnAllSchedules() {
        // given
        Long concertId = 1L;
        LocalDateTime concertDate = LocalDateTime.now().plusDays(1);

        ConcertSchedule schedule = ConcertSchedule.builder()
                .id(concertId)
                .concertDate(concertDate)
                .build();

        given(concertRepository.findById(concertId)).willReturn(Optional.of(schedule));

        // when
        List<ConcertScheduleResponse> result = concertService.getConcertSchedules(concertId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).concertScheduleId()).isEqualTo(concertId);
        assertThat(result.get(0).concertDate()).isEqualTo(concertDate);
        verify(concertRepository).findById(concertId);
    }

    @Test
    @DisplayName("공연 좌석 목록 조회 시 AVAILABLE 상태의 좌석만 반환되고 RESERVED,TEMPORARY 상태의 좌석은 제외된다")
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
                        .build(),
                Seat.builder()
                        .seatNum(4)
                        .status(SeatStatus.TEMPORARY)  // 임시예약된 좌석
                        .build()
        );

        // AVAILABLE 상태의 좌석만 필터링된 목록
        List<Seat> availableSeats = allSeats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .collect(Collectors.toList());

        given(concertRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
        given(seatRepository.findByConcertScheduleIdAndStatus(scheduleId, SeatStatus.AVAILABLE))
                .willReturn(availableSeats);

        // when
        ConcertSeatAvailableResponse result = concertService.getAvailableSeats(scheduleId);

        // then
        assertThat(result.date()).isEqualTo(concertDate);
        assertThat(result.availableSeats()).hasSize(2);  // AVAILABLE 좌석 2개만 포함
        assertThat(result.availableSeats()).containsExactly(1, 2);  // (3, 4번) 좌석 제외
        assertThat(result.availableSeats()).doesNotContain(3);  // RESERVED 좌석 미포함 확인
        assertThat(result.availableSeats()).doesNotContain(4);  // TEMPORARY 좌석 미포함 확인

        verify(concertRepository).findById(scheduleId);
        verify(seatRepository).findByConcertScheduleIdAndStatus(scheduleId, SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("스케줄 ID로 공연 스케줄 조회 시 성공적으로 조회된다")
    void getSchedule_Success() {
        // given
        Long scheduleId = 1L;
        ConcertSchedule schedule = ConcertSchedule.builder()
                .id(scheduleId)
                .concertDate(LocalDateTime.now().plusDays(1))
                .build();

        given(concertRepository.findById(scheduleId)).willReturn(Optional.of(schedule));

        // when
        ConcertSchedule result = concertService.getSchedule(scheduleId);

        // then
        assertNotNull(result);
        assertEquals(scheduleId, result.getId());
        verify(concertRepository).findById(scheduleId);
    }

    @Test
    @DisplayName("존재하지 않는 스케줄 ID로 조회 시 ConcertError 예외가 발생한다")
    void getSchedule_NotFound() {
        // given
        Long scheduleId = 999L;
        given(concertRepository.findById(scheduleId)).willReturn(Optional.empty());

        // when & then
        assertThrows(ConcertError.class, () -> concertService.getSchedule(scheduleId));
        verify(concertRepository).findById(scheduleId);
    }

    @Test
    @DisplayName("좌석 ID로 좌석 정보를 성공적으로 조회한다")
    void getSeat_Success() {
        // given
        Long seatId = 1L;
        Seat seat = Seat.builder()
                .id(seatId)
                .seatNum(1)
                .price(100000L)
                .status(SeatStatus.AVAILABLE)
                .build();

        given(seatRepository.findById(seatId)).willReturn(Optional.of(seat));

        // when
        Seat result = concertService.getSeat(seatId);

        // then
        assertNotNull(result);
        assertEquals(seatId, result.getId());
        verify(seatRepository).findById(seatId);
    }

    @Test
    @DisplayName("존재하지 않는 좌석 ID로 조회 시 ConcertError 예외가 발생한다")
    void getSeat_NotFound() {
        // given
        Long seatId = 999L;
        given(seatRepository.findById(seatId)).willReturn(Optional.empty());

        // when & then
        assertThrows(ConcertError.class, () -> concertService.getSeat(seatId));
        verify(seatRepository).findById(seatId);
    }

    @Test
    @DisplayName("AVAILABLE 상태의 좌석을 TEMPORARY 상태로 성공적으로 변경한다")
    void occupySeat_Success() {
        // given
        Seat seat = Seat.builder()
                .id(1L)
                .seatNum(1)
                .status(SeatStatus.AVAILABLE)
                .build();

        // when
        concertService.occupySeat(seat.getId());

        // then
        assertEquals(SeatStatus.TEMPORARY, seat.getStatus());
    }

    @Test
    @DisplayName("미래 날짜의 공연과 AVAILABLE 상태의 좌석으로 검증 시 성공한다")
    void validateReservationAvailability_Success() {
        // given
        ConcertSchedule schedule = ConcertSchedule.builder()
                .id(1L)
                .concertDate(LocalDateTime.now().plusDays(1))
                .build();

        Seat seat = Seat.builder()
                .id(1L)
                .seatNum(1)
                .status(SeatStatus.AVAILABLE)
                .build();

        // when & then
        assertDoesNotThrow(() ->
                concertService.validateReservationAvailability(schedule, seat));
    }

    @Test
    @DisplayName("이미 지난 공연 날짜로 예약 시도하면 ConcertError 예외가 발생한다")
    void validateReservationAvailability_ExpiredConcert() {
        // given
        ConcertSchedule schedule = ConcertSchedule.builder()
                .id(1L)
                .concertDate(LocalDateTime.now().minusDays(1))
                .build();

        Seat seat = Seat.builder()
                .id(1L)
                .seatNum(1)
                .status(SeatStatus.AVAILABLE)
                .build();

        // when & then
        assertThrows(ConcertError.class, () ->
                concertService.validateReservationAvailability(schedule, seat));
    }

    @Test
    @DisplayName("이미 점유된 좌석으로 예약 시도하면 ConcertError 예외가 발생한다")
    void validateReservationAvailability_SeatNotAvailable() {
        // given
        ConcertSchedule schedule = ConcertSchedule.builder()
                .id(1L)
                .concertDate(LocalDateTime.now().plusDays(1))
                .build();

        Seat seat = Seat.builder()
                .id(1L)
                .seatNum(1)
                .status(SeatStatus.TEMPORARY)
                .build();

        // when & then
        assertThrows(ConcertError.class, () ->
                concertService.validateReservationAvailability(schedule, seat));
    }

    @Test
    @DisplayName("좌석 상태를 RESERVED로 변경한다")
    void updateSeatStatus_Success() {
        // given
        Long seatId = 1L;
        Seat seat = Seat.builder()
                .id(seatId)
                .status(SeatStatus.TEMPORARY)
                .build();

        given(seatRepository.findById(seatId))
                .willReturn(Optional.of(seat));

        // when
        concertService.updateSeatStatus(seatId);

        // then
        assertEquals(SeatStatus.RESERVED, seat.getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 좌석 ID로 상태 변경 시도하면 예외가 발생한다")
    void updateSeatStatus_SeatNotFound() {
        // given
        Long seatId = 999L;
        given(seatRepository.findById(seatId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ConcertError.class,
                () -> concertService.updateSeatStatus(seatId));
    }


}