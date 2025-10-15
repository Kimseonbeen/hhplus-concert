package kr.hhplus.be.server.user.domain.presentation;

import io.swagger.v3.oas.annotations.Operation;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.presentation.dto.request.QueueTokenRequest;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import kr.hhplus.be.server.user.domain.model.User;
import kr.hhplus.be.server.user.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저 생성", description = "유저 회원을 생성합니다.")
    @PostMapping("/create")
    public ResponseEntity<String> createUser() {

        // 유저 생성
        User user = userService.createUser();

        return ResponseEntity.ok("유저 생성 완료");
    }
}
