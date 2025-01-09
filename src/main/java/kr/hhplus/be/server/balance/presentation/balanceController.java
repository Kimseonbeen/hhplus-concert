package kr.hhplus.be.server.balance.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.balance.domain.Balance;
import kr.hhplus.be.server.balance.domain.BalanceService;
import kr.hhplus.be.server.balance.presentation.dto.response.BalanceResponse;
import kr.hhplus.be.server.balance.presentation.dto.response.ChargeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
@Tag(name = "Balance API", description = "잔액 충전/조회 관련 API")
public class balanceController {

    private final BalanceService balanceService;

    @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액을 조회합니다")
    @GetMapping("/{userid}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userid) {

        Balance balance = balanceService.getBalance(userid);

        return ResponseEntity.ok(BalanceResponse.from(balance));
    }

    @Operation(summary = "잔액 충전", description = "사용자의 잔액을 충전합니다")
    @PostMapping("/{userid}/charge")
    public ResponseEntity<ChargeResponse> chargeBalance(@PathVariable Long userId, @RequestBody int amount) {

        balanceService.increase(userId, amount);

        Balance balance = balanceService.getBalance(userId);

        return ResponseEntity.ok(ChargeResponse.from(balance));
    }
}
