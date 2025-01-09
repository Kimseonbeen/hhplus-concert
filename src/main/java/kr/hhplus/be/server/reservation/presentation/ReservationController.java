package kr.hhplus.be.server.reservation.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.reservation.application.ReservationFacade;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.presentation.dto.request.ReservationRequest;
import kr.hhplus.be.server.reservation.presentation.dto.response.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
@Tag(name = "Reservation API", description = "콘서트 예약 관련 API")
public class ReservationController {

    private final ReservationFacade reservationFacade;

    // 콘서트 좌석 예약
    @Operation(summary = "콘서트 좌석 예약", description = "콘서트 좌석 예약합니다.")
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> createReserve(@RequestBody ReservationRequest request) {

        ReservationResult reserve = reservationFacade.reserve(request.toCommand());

        return ResponseEntity.ok(ReservationResponse.from(reserve));
    }
}
