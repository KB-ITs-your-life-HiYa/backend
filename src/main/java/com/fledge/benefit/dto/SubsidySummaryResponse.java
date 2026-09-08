package com.fledge.benefit.dto;

import com.fledge.benefit.domain.Subsidy;

// 지원금 검색·"받고 있는 지원금" 목록에서 공통으로 쓰는 요약 정보.
// 매칭 목록(SubsidyMatchResponse)과 달리 조건 계산 없이 이름 정도만 필요한 곳에서 쓴다.
public record SubsidySummaryResponse(
        Long subsidyId,
        String name,
        String orgName
) {
    public static SubsidySummaryResponse from(Subsidy subsidy) {
        return new SubsidySummaryResponse(subsidy.getId(), subsidy.getName(), subsidy.getOrgName());
    }
}
