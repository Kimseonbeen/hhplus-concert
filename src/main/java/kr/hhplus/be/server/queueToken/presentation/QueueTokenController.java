package kr.hhplus.be.server.queueToken.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.queueToken.domain.QueueTokenStatus;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/queue")
@Tag(name = "QueueToken API", description = "대기열 토큰 관련 API")
public class QueueTokenController {

    // 대기열 토큰을 발급한다.
    @Operation(summary = "대기열 토큰 발급", description = "대기열 토큰을 발급합니다.")
    @PostMapping("/token")
    public ResponseEntity<QueueTokenResponse> issueToken(@PathVariable long userId) {

        QueueTokenResponse response = QueueTokenResponse.builder()
                .token("uuid")
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        return ResponseEntity.ok(response);
    }

    // 대기열 정보를 조회한다.
    @Operation(summary = "대기열 토큰 조회", description = "대기열 토큰을 조회합니다.")
    @GetMapping("/token/{userId}")
    public ResponseEntity<QueueTokenResponse> getQueueToken(@PathVariable long userId,
                                                            @Parameter(description = "대기열 토큰", required = true) @RequestHeader("Auth") String token) {

        QueueTokenResponse response = QueueTokenResponse.builder()
                .token("uuid")
                .status(QueueTokenStatus.WAITING)
                .num(5)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        return ResponseEntity.ok(response);
    }

}
