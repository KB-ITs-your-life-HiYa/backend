package com.fledge.member.domain;

/**
 * 로그인 계정 종류.
 * 같은 /auth/login 을 쓰되, 프론트가 이 값으로 청년 앱 / 상담사 포털을 가른다.
 */
public enum MemberRole {
    YOUTH,
    COUNSELOR
}
