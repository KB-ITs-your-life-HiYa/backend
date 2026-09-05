package com.fledge.housing.dto;

import com.fledge.housing.domain.HousingNoticeUnit;

/**
 * 공고에 딸린 단지 한 줄.
 * 값이 없는 필드는 null 로 내려 프론트가 그 줄을 숨길 수 있게 한다.
 */
public record HousingNoticeUnitResponse(
        Long id,
        String complexName,
        String region,
        String district,
        String fullAddress,
        String heatingType,
        Integer totalHouseholds,
        Integer supplyCount,
        Long deposit,
        Long monthlyRent,
        Long contractDeposit,
        Long balance
) {
    public static HousingNoticeUnitResponse from(HousingNoticeUnit unit) {
        return new HousingNoticeUnitResponse(
                unit.getId(),
                blankToNull(unit.getHsmpNm()),
                blankToNull(unit.getBrtcNm()),
                blankToNull(unit.getSignguNm()),
                blankToNull(unit.getFullAdres()),
                blankToNull(unit.getHeatMthdNm()),
                unit.getTotHshldCo(),
                unit.getSumSuplyCo(),
                unit.getRentGtn(),
                unit.getMtRntchrg(),
                unit.getEnty(),
                unit.getSurlus()
        );
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
