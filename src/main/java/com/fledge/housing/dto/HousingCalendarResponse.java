package com.fledge.housing.dto;

import java.util.List;

public record HousingCalendarResponse(
        List<HousingNoticeSummary> notices,
        String appliedRegionCode,   // 실제로 적용된 지역. null 이면 전국
        String message              // 자동 전환 등 사용자에게 보여줄 안내. 없으면 null
) {
    public static HousingCalendarResponse of(List<HousingNoticeSummary> notices) {
        return new HousingCalendarResponse(notices, null, null);
    }

    public static HousingCalendarResponse of(List<HousingNoticeSummary> notices, String regionCode) {
        return new HousingCalendarResponse(notices, regionCode, null);
    }

    public static HousingCalendarResponse fallbackToNationwide(List<HousingNoticeSummary> notices, String message) {
        return new HousingCalendarResponse(notices, null, message);
    }
}
