package com.fledge.benefit.dto;

import com.fledge.benefit.domain.Subsidy;

// 지원금 검색·"받고 있는 지원금" 목록에서 공통으로 쓰는 요약 정보.
// 매칭 목록(SubsidyMatchResponse)과 달리 조건 계산 없이 이름 정도만 필요한 곳에서 쓴다.
// regionLabel: 이름이 같은 지역별 지원금(예: "OO 자립수당 지급"이 시·도마다 따로 있는 경우)을
// 목록에서 구분하기 위한 값. orgName이 없을 때 프론트에서 대신 보여준다.
public record SubsidySummaryResponse(
        Long subsidyId,
        String name,
        String orgName,
        String regionLabel
) {
    public static SubsidySummaryResponse from(Subsidy subsidy, String regionLabel) {
        return new SubsidySummaryResponse(subsidy.getId(), subsidy.getName(), subsidy.getOrgName(), regionLabel);
    }
}
