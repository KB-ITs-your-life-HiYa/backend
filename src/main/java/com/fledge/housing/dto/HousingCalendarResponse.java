package com.fledge.housing.dto;

import java.util.List;

/**
 * 캘린더 화면 하나가 필요로 하는 것을 통째로 담는다.
 *
 * notices 와 ongoingNotices 를 나누는 이유:
 * 접수기간이 아주 긴 공고(예: 2월 시작 12월 마감)는 조회 중인 달 어느 날짜에도
 * 시작·마감 점을 찍을 자리가 없다. 그렇다고 목록에서 빼면 신청 가능한 공고가
 * 화면에서 통째로 사라진다. 그래서 "점을 찍을 수 있는 것"과 "상시 모집으로
 * 카드에 따로 보여줄 것"을 서버가 미리 나눠서 내려준다.
 */
public record HousingCalendarResponse(
        List<HousingNoticeSummary> notices,          // 캘린더에 점을 찍을 수 있는 공고
        List<HousingNoticeSummary> ongoingNotices,    // 상시 모집. 이번 달에 시작도 마감도 없는 초장기 공고
        String appliedRegionCode,                     // 실제로 적용된 지역. null 이면 전국
        String message                                 // 자동 전환 등 사용자에게 보여줄 안내. 없으면 null
) {
}
