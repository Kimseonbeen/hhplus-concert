package kr.hhplus.be.server.user.domain;

import kr.hhplus.be.server.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(400, "USER_NOT_FOUND", "해당 유저가 존재하지 않습니다."));
    }

}
