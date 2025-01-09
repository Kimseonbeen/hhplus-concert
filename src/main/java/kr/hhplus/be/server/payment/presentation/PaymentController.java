package kr.hhplus.be.server.payment.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.payment.application.PaymentFacade;
import kr.hhplus.be.server.payment.application.dto.PaymentResult;
import kr.hhplus.be.server.payment.presentation.dto.request.PaymentRequest;
import kr.hhplus.be.server.payment.presentation.dto.response.PaymentResponse;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.presentation.dto.response.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment API", description = "콘서트 결제 관련 API")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @Operation(summary = "콘서트 좌석 결제", description = "콘서트 좌석을 결제합니다")
    @PostMapping("/concertPayment")
    public ResponseEntity<PaymentResponse> createPayment(@RequestHeader("Auth") String token,
                                                         @RequestBody PaymentRequest request) {

        PaymentResult payment = paymentFacade.completePayment(token, request.toCommand());

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
}
