package com.fledge.member.dto;

import com.fledge.member.domain.Member;

public record LoginResponse(
        String token,
        MemberResponse member
) {
    public static LoginResponse of(Member member, String token) {
        return new LoginResponse(token, MemberResponse.from(member));
    }
}
