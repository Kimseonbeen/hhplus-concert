
package kr.hhplus.be.server.concert.domain.service;

import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.model.ConcertScheduleStatus;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
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

    private final Long TEST_CONCERT_ID = 1L;
    private LocalDateTime FUTURE_DATE; // 필드 선언만 남기고 초기화 제거

    @BeforeEach
        // 테스트 실행 전마다 FUTURE_DATE 초기화
    void setUp() {
        // 테스트 실행 시점에 현재 시간 기준 +1일로 설정하여 일관성을 유지합니다.
        FUTURE_DATE = LocalDateTime.now().plusDays(1);
    }

    @Test
    @DisplayName("성공: 콘서트 ID로 조회 시 예약 가능한 일정만 DTO로 변환되어 반환된다")
    void getConcertSchedules_ShouldReturnAvailableSchedules() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ConcertSchedule> availableSchedules = List.of(
                new ConcertSchedule(100L, TEST_CONCERT_ID, FUTURE_DATE.plusHours(1), ConcertScheduleStatus.AVAILABLE),
                new ConcertSchedule(101L, TEST_CONCERT_ID, FUTURE_DATE.plusHours(2), ConcertScheduleStatus.AVAILABLE)
        );
        Page<ConcertSchedule> page = new PageImpl<>(availableSchedules, pageable, availableSchedules.size());

        given(concertScheduleRepository.findAvailableSchedule(TEST_CONCERT_ID, pageable)).willReturn(page);

        // when
        Page<ConcertScheduleResponse> result = concertService.getConcertSchedules(TEST_CONCERT_ID, pageable);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.getContent().get(0).concertScheduleId()).isEqualTo(100L);
        verify(concertScheduleRepository).findAvailableSchedule(TEST_CONCERT_ID, pageable);
    }
    
    @Test
    @DisplayName("실패 : 콘서트 ID로 조회 시 예약 가능한 일정이 없으면 CONCERT_NOT_FOUND 예외를 발생시킨다")
    void getConcertSchedules_ShouldThrowException_WhenNoSchedulesFound() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ConcertSchedule> emptyPage = Page.empty(pageable);

        given(concertScheduleRepository.findAvailableSchedule(TEST_CONCERT_ID, pageable)).willReturn(emptyPage);

        // when
        ConcertException exception = assertThrows(ConcertException.class, () -> {
            concertService.getConcertSchedules(TEST_CONCERT_ID, pageable);
        });

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ConcertErrorCode.CONCERT_NOT_FOUND.getCode());
        assertThat(exception.getStatus()).isEqualTo(ConcertErrorCode.CONCERT_NOT_FOUND.getStatus());
        verify(concertScheduleRepository).findAvailableSchedule(TEST_CONCERT_ID, pageable);

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
        List<Integer> availableSeats = allSeats.stream()
                        .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                        .map(Seat::getSeatNum)
                        .toList();

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