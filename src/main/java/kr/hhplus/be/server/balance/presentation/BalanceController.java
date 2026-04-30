package kr.hhplus.be.server.balance.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.balance.domain.model.Balance;
import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.balance.presentation.dto.request.ChargeRequest;
import kr.hhplus.be.server.balance.presentation.dto.response.BalanceResponse;
import kr.hhplus.be.server.balance.presentation.dto.response.ChargeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
@Tag(name = "Balance API", description = "잔액 충전/조회 관련 API")
public class BalanceController {

    private final BalanceService balanceService;

    @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다")
    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userId) {

        Balance balance = balanceService.getBalance(userId);

        return ResponseEntity.ok(BalanceResponse.from(balance));
    }

    @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다")
    @PostMapping("/{userId}/charge")
    public ResponseEntity<ChargeResponse> chargeBalance(@PathVariable Long userId, @RequestBody ChargeRequest request) {

        balanceService.increase(userId, request.amount());

        Balance balance = balanceService.getBalance(userId);

        return ResponseEntity.ok(ChargeResponse.from(balance));
    }
}
