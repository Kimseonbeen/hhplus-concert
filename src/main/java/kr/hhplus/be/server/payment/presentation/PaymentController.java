package kr.hhplus.be.server.payment.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.payment.presentation.dto.request.PaymentRequest;
import kr.hhplus.be.server.payment.presentation.dto.response.PaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@Tag(name = "Payment API", description = "콘서트 결제 관련 API")
public class PaymentController {

    @Operation(summary = "콘서트 좌석 결제", description = "콘서트 좌석을 결제합니다")
    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request,
                                                @Parameter(description = "대기열 토큰", required = true) @RequestHeader("Auth") String token) {

        PaymentResponse response = PaymentResponse.builder()
                .message("결제에 성공했습니다.")
                .paymentId(1L)
                .build();

        return ResponseEntity.ok(response);
    }
}
