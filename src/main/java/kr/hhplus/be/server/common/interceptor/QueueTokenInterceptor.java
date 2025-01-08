package kr.hhplus.be.server.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.hhplus.be.server.queueToken.domain.QueueToken;
import kr.hhplus.be.server.queueToken.domain.QueueTokenService;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenError;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenErrorCode;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class QueueTokenInterceptor implements HandlerInterceptor {

    private final QueueTokenService queueTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Auth");
        if (token == null || token.isEmpty()) {
            throw new QueueTokenError(QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND);
        }

        // 토큰 검증
        // 토큰 상태 체크
        queueTokenService.validateToken(token);

        return true;
    }

}
