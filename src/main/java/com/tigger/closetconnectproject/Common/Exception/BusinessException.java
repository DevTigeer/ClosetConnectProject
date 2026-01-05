package com.tigger.closetconnectproject.Common.Exception;

/**
 * 비즈니스 로직 검증 실패 시 발생하는 예외
 * HTTP 400 Bad Request로 매핑됩니다.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
