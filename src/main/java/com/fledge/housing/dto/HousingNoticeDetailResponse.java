package com.fledge.housing.dto;

import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;

import java.time.LocalDate;
import java.util.List;

/**
 * 공고 상세 화면용 응답.
 * 접수기간·공급기관·유형·단지 목록·문의처·원문 링크를 담는다.
 * 값이 없는 항목은 null 이라 프론트에서 줄을 숨긴다.
 */
public record HousingNoticeDetailResponse(
        Long id,
        String title,
        String institution,
        String houseType,
        String supplyType,
        TargetType targetType,
        LocalDate announceDate,
        LocalDate beginDate,
        LocalDate endDate,
        LocalDate winnerAnnounceDate,
        String contact,
        String applyUrl,
        String myhomeUrl,
        boolean superseded,
        List<HousingNoticeUnitResponse> units
) {
    public static HousingNoticeDetailResponse from(HousingNotice notice,
                                                   List<HousingNoticeUnitResponse> units) {
        return new HousingNoticeDetailResponse(
                notice.getId(),
                blankToNull(notice.getPblancNm()),
                blankToNull(notice.getSuplyInsttNm()),
                blankToNull(notice.getHouseTyNm()),
                blankToNull(notice.getSuplyTyNm()),
                notice.getTargetType(),
                notice.getRcritPblancDe(),
                notice.getBeginDe(),
                notice.getEndDe(),
                notice.getPrzwnerPresnatnDe(),
                blankToNull(notice.getRefrnc()),
                blankToNull(notice.getUrl()),
                blankToNull(notice.getPcUrl()),
                notice.isSuperseded(),
                units
        );
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
