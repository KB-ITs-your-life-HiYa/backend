package com.fledge.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 에러 코드.
 *
 * 도메인 에러를 추가할 때는 {@code <도메인>_<사유>} 형태로 짓는다.
 * 예) HOUSING_NOTICE_NOT_FOUND, CARE_SIGNAL_ALREADY_HANDLED
 *
 * message 는 사용자에게 그대로 보여줄 수 있는 문장으로 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),

   // habit — 놀이 탭(금융습관 트레이닝)
    HABIT_QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "오늘의 퀴즈를 찾을 수 없습니다"),
    HABIT_QUIZ_ALREADY_ANSWERED(HttpStatus.CONFLICT, "오늘의 퀴즈는 이미 풀었습니다"),
    HABIT_QUIZ_OPTION_INVALID(HttpStatus.BAD_REQUEST, "선택한 보기가 올바르지 않습니다"),
    HABIT_TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, "토픽을 찾을 수 없습니다"),
    HABIT_PUZZLE_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "퍼즐 세트를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;
}
