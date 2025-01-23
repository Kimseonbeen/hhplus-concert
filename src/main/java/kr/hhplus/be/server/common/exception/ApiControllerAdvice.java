package kr.hhplus.be.server.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    private final HttpServletRequest request;

    @ExceptionHandler(value = {CustomException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(CustomException e) {

        String errorLog = String.format("[%s] ErrorCode: %s, Message: %s, Path: %s",
                e.getClass().getSimpleName(),
                e.getErrorCode(),
                e.getMessage(),
                request.getRequestURI() // 에러 발생 경로
        );

        switch (e.getLogLevel()) {
            case ERROR -> log.error(errorLog);
            case WARN -> log.warn(errorLog);
            default -> log.info(errorLog);
        }

        return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", e.getMessage()));
    }
}
