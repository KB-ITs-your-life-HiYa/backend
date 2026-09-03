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
    CARE_SIGNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "상담을 찾을 수 없습니다"),
    CARE_SIGNAL_RESOLVED(HttpStatus.CONFLICT, "이미 해결된 상담입니다. 새로고침해 주세요"),
    CARE_INVALID_CHANGE(HttpStatus.BAD_REQUEST, "날짜는 1~31일, 금액은 1원 이상으로 입력하고 변경할 항목을 선택해 주세요"),
    CARE_DEMO_FORBIDDEN(HttpStatus.FORBIDDEN, "시연 기능은 로컬 개발 환경의 demo2만 사용할 수 있습니다"),
    CARE_DEMO_DATE(HttpStatus.BAD_REQUEST, "9월 23일, 24일, 26일, 10월 1일 순으로 진행해 주세요. 이전 날짜는 초기화가 필요합니다"),
    CARE_REFERRAL_NOT_ELIGIBLE(HttpStatus.CONFLICT, "현재는 담당자 연결 요청 조건에 해당하지 않습니다"),
    CARE_DEMO_RESET_FAILED(HttpStatus.CONFLICT, "시연 데이터 초기화에 실패했습니다. 계좌·일정 충돌 또는 기존 데이터 참조를 확인해 주세요"),
    CARE_REQUEST_CONFLICT(HttpStatus.CONFLICT, "같은 요청 번호로 다른 답변을 보낼 수 없습니다");

    private final HttpStatus status;
    private final String message;
}
