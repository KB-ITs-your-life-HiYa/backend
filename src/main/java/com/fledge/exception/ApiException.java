package com.fledge.exception;

import com.fledge.common.ErrorCode;
import lombok.Getter;

/**
 * 서비스에서 실패를 알릴 때 던지는 예외.
 * {@code GlobalExceptionHandler} 가 받아서 공통 실패 응답으로 바꾼다.
 *
 * <pre>
 * throw new ApiException(ErrorCode.NOT_FOUND);
 * throw new ApiException(ErrorCode.NOT_FOUND, "공고를 찾을 수 없습니다");
 * </pre>
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
