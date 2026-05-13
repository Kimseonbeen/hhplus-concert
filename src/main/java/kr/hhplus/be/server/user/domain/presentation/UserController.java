package kr.hhplus.be.server.user.domain.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.user.domain.model.User;
import kr.hhplus.be.server.user.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User API", description = "유저 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저 생성", description = "유저를 생성합니다.")
    @PostMapping("/create")
    public ResponseEntity<Long> createUser() {

        User user = userService.createUser();

        return ResponseEntity.ok(user.getId());
    }
}
