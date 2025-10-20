package kr.hhplus.be.server.concert.domain.repository;

import kr.hhplus.be.server.concert.domain.model.ConcertSchedule;
import kr.hhplus.be.server.concert.domain.model.ConcertScheduleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ConcertScheduleRepositoryTest {

    @Autowired
    ConcertScheduleRepository concertScheduleRepository;

    final Long TEST_CONCERT_ID = 1L;
    final Long OTHER_CONCERT_ID = 2L;
    LocalDateTime FUTURE_DATE;
    LocalDateTime PAST_DATE;

    @BeforeEach
    void setUp() {
        // 테스트 시점 기준 날짜 설정
        FUTURE_DATE = LocalDateTime.now().plusDays(1);
        PAST_DATE = LocalDateTime.now().minusDays(1);

        // 테스트를 위해 다양한 조건의 스케줄을 DB에 저장합니다.
        // 저장 시 Repository의 save() 메서드를 사용해야 합니다. (편의상 saveAndFlush를 사용)

        // 1. 성공 케이스 (AVAILABLE & 미래 날짜) -> 반환되어야 함
        concertScheduleRepository.saveAndFlush(ConcertSchedule.builder()
                .concertId(TEST_CONCERT_ID)
                .concertDate(FUTURE_DATE.plusHours(1))
                .status(ConcertScheduleStatus.AVAILABLE)
                .build()); // ID: 1
        concertScheduleRepository.saveAndFlush(ConcertSchedule.builder()
                .concertId(TEST_CONCERT_ID)
                .concertDate(FUTURE_DATE.plusHours(2))
                .status(ConcertScheduleStatus.AVAILABLE)
                .build()); // ID: 2

        // 2. 상태 제외 케이스 (SOLDOUT & 미래 날짜) -> 반환되지 않아야 함
        concertScheduleRepository.saveAndFlush(ConcertSchedule.builder()
                .concertId(TEST_CONCERT_ID)
                .concertDate(FUTURE_DATE.plusHours(3))
                .status(ConcertScheduleStatus.SOLDOUT)
                .build()); // ID: 3

        // 3. 날짜 제외 케이스 (AVAILABLE & 과거 날짜) -> 반환되지 않아야 함
        concertScheduleRepository.saveAndFlush(ConcertSchedule.builder()
                .concertId(TEST_CONCERT_ID)
                .concertDate(PAST_DATE)
                .status(ConcertScheduleStatus.AVAILABLE)
                .build()); // ID: 4

        // 4. ID 제외 케이스 (다른 콘서트 ID) -> 반환되지 않아야 함
        concertScheduleRepository.saveAndFlush(ConcertSchedule.builder()
                .concertId(OTHER_CONCERT_ID)
                .concertDate(FUTURE_DATE.plusHours(4))
                .status(ConcertScheduleStatus.AVAILABLE)
                .build()); // ID: 5
    }

    @Test
    @DisplayName("쿼리: 'AVAILABLE' 상태이고 '미래 날짜'인 일정만 정확히 반환한다")
    void findAvailableSchedule_ShouldFilterByStatusAndDate() {
        // When
        List<ConcertSchedule> result = concertScheduleRepository.findAvailableSchedule(TEST_CONCERT_ID);

        // Then
        // 1. 결과는 오직 성공 케이스 2개만 반환해야 합니다.
        assertThat(result).hasSize(2);

        // 2. 반환된 모든 스케줄이 실제로 AVAILABLE 상태인지 검증
        assertThat(result)
                // extracting : 👈 ConcertSchedule 객체 리스트에서 'status' 필드만 추출
                .extracting(ConcertSchedule::getStatus)
                .containsOnly(ConcertScheduleStatus.AVAILABLE);

        // 3. 반환된 모든 스케줄의 날짜가 현재 시간보다 미래인지 검증
        assertThat(result)
                .extracting(ConcertSchedule::getConcertDate)
                .allMatch(date -> date.isAfter(LocalDateTime.now()));

        // 4. 반환된 모든 스케쥴의 ID가 TEST_CONCERT_ID와 일치하는지 검증
        assertThat(result)
                .extracting(ConcertSchedule::getConcertId)
                .containsOnly(TEST_CONCERT_ID);

        // 5. 반환된 스케쥴의 ID가 1 또는 2인지 검증
        assertThat(result)
                .extracting(ConcertSchedule::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }
}
