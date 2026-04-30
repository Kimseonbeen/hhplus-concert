package kr.hhplus.be.server.queueToken.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.queueToken.application.QueueTokenFacade;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import kr.hhplus.be.server.queueToken.presentation.dto.request.QueueTokenRequest;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Tag(name = "QueueToken API", description = "대기열 토큰 관련 API")
public class QueueTokenController {
    private final QueueTokenFacade queueTokenFacade;
    private final QueueTokenService queueTokenService;

    // 대기열 토큰을 발급한다.
    @Operation(summary = "대기열 토큰 발급", description = "대기열 토큰을 발급합니다.")
    @PostMapping("/token")
    public ResponseEntity<QueueTokenResponse> createToken(@RequestBody QueueTokenRequest request) {
        // 토큰 생성
        QueueToken queueToken = queueTokenFacade.createToken(request.userId());

        return ResponseEntity.ok(QueueTokenResponse.from(queueToken));
    }

    // 대기열 정보를 조회한다.
    @Operation(summary = "대기열 토큰 조회", description = "소지한 토큰의 현재 상태(Active/Waiting)와 순번을 조회합니다.")
    @GetMapping("/status")
    public ResponseEntity<QueueTokenResponse> getQueueToken(@Parameter(description = "대기열 토큰", required = true)
                                                            @RequestHeader("TOKEN") String token) {

        return ResponseEntity.ok(queueTokenService.getQueueToken(token));
    }
}
