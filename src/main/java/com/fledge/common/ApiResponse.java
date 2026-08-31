package com.fledge.common;

/**
 * 모든 API 응답의 공통 형식.
 *
 * <pre>
 * 성공  { "success": true,  "data": {...}, "error": null }
 * 실패  { "success": false, "data": null,  "error": { "code": "...", "message": "..." } }
 * </pre>
 *
 * 컨트롤러는 {@link #ok(Object)} 만 쓴다.
 * 실패 응답은 {@code GlobalExceptionHandler} 가 만든다.
 */
public record ApiResponse<T>(boolean success, T data, Error error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(ErrorCode code, String message) {
        return new ApiResponse<>(false, null, new Error(code.name(), message));
    }

    public record Error(String code, String message) {
    }
}
