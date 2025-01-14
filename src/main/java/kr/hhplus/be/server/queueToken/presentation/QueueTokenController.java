package kr.hhplus.be.server.queueToken.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.queueToken.application.QueueTokenFacade;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
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

    // 대기열 토큰을 발급한다.
    @Operation(summary = "대기열 토큰 발급", description = "대기열 토큰을 발급합니다.")
    @PostMapping("/token")
    public ResponseEntity<QueueTokenResponse> issueToken(@RequestBody QueueTokenRequest request) {

        // 토큰 생성
        QueueToken queueToken = queueTokenFacade.issueQueueToken(request.userId());


        QueueTokenResponse response = QueueTokenResponse.builder()
                .expiredAt(queueToken.getExpiredAt())
                .token(queueToken.getToken())
                .status(queueToken.getStatus())
                .build();

        return ResponseEntity.ok(response);
    }

    // 대기열 정보를 조회한다.
    @Operation(summary = "대기열 토큰 조회", description = "대기열 토큰을 조회합니다.")
    @GetMapping("/token/{userId}")
    public ResponseEntity<QueueTokenResponse> getQueueToken(@PathVariable long userId,
                                                            @Parameter(description = "대기열 토큰", required = true) @RequestHeader("Auth") String token) {

        return ResponseEntity.ok(queueTokenFacade.getQueueTokenStatus(token, userId));
    }

}
