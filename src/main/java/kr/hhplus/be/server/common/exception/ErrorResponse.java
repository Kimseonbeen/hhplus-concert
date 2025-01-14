package kr.hhplus.be.server.common.exception;

public record ErrorResponse(
        String code,
        String message
) {
}