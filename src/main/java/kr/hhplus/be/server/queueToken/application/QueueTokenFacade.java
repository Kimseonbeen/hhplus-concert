package kr.hhplus.be.server.queueToken.application;

import kr.hhplus.be.server.queueToken.domain.QueueToken;
import kr.hhplus.be.server.queueToken.domain.QueueTokenService;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import kr.hhplus.be.server.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueueTokenFacade {

    private final QueueTokenService queueTokenService;
    private final UserService userService;

    @Transactional
    public QueueToken issueQueueToken(long userId) {
        // 1. 존재하는 유저인지 확인 한다.
        userService.findById(userId);

        // 2. 토큰 생성
        return queueTokenService.issueQueueToken(userId);
    }

    public QueueTokenResponse getQueueTokenStatus(String token, long userId) {
        // 1. 존재하는 유저인지 확인 한다.
        userService.findById(userId);

        // 2. 토큰 반환
        return queueTokenService.getQueueToken(token);
    }


}
