package kr.hhplus.be.server.concert.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.concert.domain.ConcertService;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/concert")
@Tag(name = "ConcertSchedule API", description = "콘서트 예약 가능 관련 API")
public class ConcertController {

    private final ConcertService concertService;

    // 예약 가능 날짜 목록 조회
    @Operation(summary = "예약 가능 날짜 조회", description = "예약 가능한 날짜을 조회합니다")
    @GetMapping("/{concertId}/schedules")
    public ResponseEntity<List<ConcertScheduleResponse>> getConcertSchedule(@PathVariable long concertId) {


        // 공연 일정 조회
        List<ConcertScheduleResponse> response = concertService.getConcertSchedules(concertId);

        return ResponseEntity.ok(response);
    }

    // 예약 가능 콘서트 좌석 목록 조회
    @Operation(summary = "예약 가능 좌석 조회", description = "예약 가능한 좌석을 조회합니다")
    @GetMapping("/{concertScheduleId}/seats")
    public ResponseEntity<ConcertSeatAvailableResponse> getConcertScheduleSeat(@PathVariable long concertScheduleId) {

        // 예약 가능 좌석 조회
        ConcertSeatAvailableResponse response = concertService.getAvailableSeats(concertScheduleId);

        return ResponseEntity.ok(response);
    }
}
