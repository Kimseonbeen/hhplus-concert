package kr.hhplus.be.server.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
public class AccessLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper cachingRequestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachingResponseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        try {
            filterChain.doFilter(cachingRequestWrapper, cachingResponseWrapper);
        } finally {
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            int status = response.getStatus();

            // Request/Response Body 가져오기
            String requestBody = new String(cachingRequestWrapper.getContentAsByteArray(),
                    request.getCharacterEncoding());
            String responseBody = new String(cachingResponseWrapper.getContentAsByteArray(),
                    response.getCharacterEncoding());

            log.info("method: {}, uri: {}, requestBody: {}, status: {}, responseBody: {}, latency: {}ms",
                    method,
                    requestURI,
                    requestBody,
                    status,
                    responseBody,
                    latency);

            cachingResponseWrapper.copyBodyToResponse();  // 응답 body 복사
        }
    }
}
