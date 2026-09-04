package com.fledge.member.dto;

import com.fledge.member.domain.EligibilityTier;
import com.fledge.member.domain.Member;

public record MemberResponse(
        Long memberId,
        String email,
        int age,
        EligibilityTier tier,
        String tierLabel,
        Long daysUntilSupportEnd
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getAge(),
                member.getEligibilityTier(),
                member.getEligibilityTier().getLabel(),
                member.getDaysUntilSupportEnd()
        );
    }
}