package com.fledge.member.dto;

import com.fledge.member.domain.EligibilityTier;
import com.fledge.member.domain.Member;
import com.fledge.member.domain.MemberRole;

import java.time.LocalDate;

public record MemberResponse(
        Long memberId,
        String email,
        MemberRole role,
        int age,
        EligibilityTier tier,
        String tierLabel,
        Long daysUntilSupportEnd,
        LocalDate protectionEndDate,
        String homeRegionCode,
        String regionName
) {
    public static MemberResponse from(Member member, String regionName) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getRole(),
                member.getAge(),
                member.getEligibilityTier(),
                member.getEligibilityTier().getLabel(),
                member.getDaysUntilSupportEnd(),
                member.getProtectionEndDate(),
                member.getHomeRegionCode(),
                regionName
        );
    }
}
