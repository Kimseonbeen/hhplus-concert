package kr.hhplus.be.server.concert.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/concert")
@Tag(name = "ConcertSchedule API", description = "콘서트 예약 가능 관련 API")
public class ConcertController {

    // 예약 가능 날짜 목록 조회
    @Operation(summary = "예약 가능 날짜 조회", description = "예약 가능한 날짜을 조회합니다")
    @GetMapping("/schedules")
    public ResponseEntity<List<ConcertScheduleResponse>> getConcertSchedule(@PathVariable long userId,
                                                                            @Parameter(description = "대기열 토큰", required = true)
                                                                            @RequestHeader("Auth") String token) {

        ConcertScheduleResponse concertSchedule1 = ConcertScheduleResponse.builder()
                .scheduleId(1L)
                .concertDate(LocalDateTime.now().plusDays(1))
                .build();

        ConcertScheduleResponse concertSchedule2 = ConcertScheduleResponse.builder()
                .scheduleId(1L)
                .concertDate(LocalDateTime.now().plusDays(3))
                .build();

        List<ConcertScheduleResponse> response = List.of(concertSchedule1, concertSchedule2);

        return ResponseEntity.ok(response);
    }

    // 예약 가능 콘서트 좌석 목록 조회
    @Operation(summary = "예약 가능 좌석 조회", description = "예약 가능한 좌석을 조회합니다")
    @GetMapping("/seats")
    public ResponseEntity<ConcertSeatAvailableResponse> getConcertScheduleSeat(@PathVariable long userId,
                                                                               @Parameter(description = "대기열 토큰", required = true)
                                                                               @RequestHeader("Auth") String token) {

        ConcertSeatAvailableResponse response = ConcertSeatAvailableResponse.builder()
                .date(LocalDateTime.now())
                .availableSeats(List.of(1, 2, 3, 4, 5))
                .build();

        return ResponseEntity.ok(response);
    }
}
