package kr.hhplus.be.server.concert.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.concert.domain.service.ConcertService;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/concert")
@Tag(name = "ConcertSchedule API", description = "콘서트 예약 가능 관련 API")
public class ConcertController {

    private final ConcertService concertService;

    // 예약 가능 날짜 목록 조회
    @Operation(summary = "예약 가능 날짜 조회", description = "예약 가능한 날짜을 조회합니다")
    @GetMapping("/{concertId}/schedules")
    public ResponseEntity<Page<ConcertScheduleResponse>> getConcertSchedule(
            @PathVariable Long concertId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(concertService.getConcertSchedules(concertId, pageable));
    }

    // 예약 가능 콘서트 좌석 목록 조회
    @Operation(summary = "예약 가능 좌석 조회", description = "예약 가능한 좌석을 조회합니다")
    @GetMapping("/{concertScheduleId}/seats")
    public ResponseEntity<ConcertSeatAvailableResponse> getConcertScheduleSeat(@PathVariable Long concertScheduleId) {

        // 예약 가능 좌석 조회
        ConcertSeatAvailableResponse response = concertService.getAvailableSeats(concertScheduleId);

        return ResponseEntity.ok(response);
    }
}
