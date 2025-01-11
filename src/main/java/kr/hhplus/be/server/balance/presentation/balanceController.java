package kr.hhplus.be.server.balance.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.balance.presentation.dto.request.ChargeRequest;
import kr.hhplus.be.server.balance.presentation.dto.response.BalanceResponse;
import kr.hhplus.be.server.balance.presentation.dto.response.ChargeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/balance")
@Tag(name = "Balance API", description = "잔액 충전/조회 관련 API")
public class balanceController {

    @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다")
    @GetMapping("/{userid}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String userid,
                                                      @Parameter(description = "대기열 토큰", required = true)
                                                      @RequestHeader("Auth") String token) {
        BalanceResponse response = BalanceResponse.builder()
                .userId(1L)
                .balance("1000")
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다")
    @PostMapping("/{userid}/charge")
    public ResponseEntity<ChargeResponse> chargeBalance(@PathVariable String userid,
                                                        @Parameter(description = "대기열 토큰", required = true)
                                                        @RequestHeader("Auth") String token
    ) {
        ChargeResponse response = ChargeResponse.builder()
                .message("잔액 충전이 완료되었습니다.")
                .balance("2000")
                .build();

        return ResponseEntity.ok(response);
    }
}
