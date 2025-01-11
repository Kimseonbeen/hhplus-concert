package kr.hhplus.be.server.reservation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.reservation.presentation.dto.request.ReservationRequest;
import kr.hhplus.be.server.reservation.presentation.dto.response.ReservationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservation")
@Tag(name = "Reservation API", description = "콘서트 예약 관련 API")
public class ReservationController {

    // 콘서트 좌석 예약
    @Operation(summary = "콘서트 좌석 예약", description = "콘서트 좌석 예약합니다.")
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> createReserve(@RequestBody ReservationRequest request,
                                                @Parameter(description = "대기열 토큰", required = true)
                                                @RequestHeader("Auth") String token) {

        ReservationResponse response = ReservationResponse.builder()
                .message("좌석 예약에 성공했습니다.")
                .reservationId(1L)
                .build();

        return ResponseEntity.ok(response);
    }
}
