package com.tigger.closetconnectproject.Common.Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * - Controller에서 발생한 예외를 일관된 형식으로 변환하여 응답
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 리소스를 찾을 수 없을 때 발생하는 예외 처리 (404 Not Found)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "Not Found",
                        "message", e.getMessage()
                ));
    }

    /**
     * 비즈니스 로직 검증 실패 시 발생하는 예외 처리 (400 Bad Request)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad Request");
        body.put("message", e.getMessage());
        if (e.getErrorCode() != null) {
            body.put("errorCode", e.getErrorCode());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    /**
     * IllegalArgumentException 처리 (하위 호환성 유지)
     * - 점진적으로 ResourceNotFoundException, BusinessException으로 전환 권장
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        // "찾을 수 없", "not found" 등의 메시지는 404, 나머지는 400
        String message = e.getMessage();
        boolean isNotFound = message != null &&
            (message.contains("찾을 수 없") || message.contains("not found") ||
             message.contains("없음") || message.contains("존재하지 않는"));

        int status = isNotFound ? 404 : 400;

        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", isNotFound ? "Not Found" : "Bad Request",
                        "message", message
                ));
    }

    /**
     * 설명: Validation 실패 시 발생 (@Valid 어노테이션)
     * - 각 필드별 에러 메시지를 Map으로 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Validation Failed",
                        "message", "입력값이 올바르지 않습니다.",
                        "fields", errors
                ));
    }

    /**
     * 설명: 접근 권한이 없을 때 발생 (Spring Security)
     * - 작성자가 아닌 사용자가 수정/삭제 시도 등
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "Forbidden",
                        "message", e.getMessage() != null ? e.getMessage() : "권한이 없습니다."
                ));
    }

    /**
     * 설명: 인증 실패 시 발생 (잘못된 비밀번호 등)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "Unauthorized",
                        "message", e.getMessage() != null ? e.getMessage() : "인증에 실패했습니다."
                ));
    }

    /**
     * 설명: 기타 예외 처리 (예상하지 못한 에러)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        // 실제 운영 환경에서는 e.getMessage() 노출 금지, 로그만 기록
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", "서버 오류가 발생했습니다."
                ));
    }
}
