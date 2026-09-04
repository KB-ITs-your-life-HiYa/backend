package com.fledge.exception;

import com.fledge.common.ApiResponse;
import com.fledge.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 실패 응답을 여기서 만든다. 컨트롤러에서 직접 실패 응답을 만들지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.INVALID_REQUEST,
                ErrorCode.INVALID_REQUEST.getMessage()));
    }

    /** 서비스가 의도적으로 던진 실패 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code, e.getMessage()));
    }

    /** @Valid 검증 실패 — 어느 필드가 왜 틀렸는지 함께 알려준다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST, detail));
    }

    /** 없는 경로 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getStatus())
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage()));
    }

    /** GET 자리에 POST 를 보내는 등 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage()));
    }

    /** 나머지 전부. 원인은 로그로 남기고 응답에는 내부 정보를 노출하지 않는다 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
