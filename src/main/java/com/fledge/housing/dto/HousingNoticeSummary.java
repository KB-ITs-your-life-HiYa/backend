package com.fledge.housing.dto;

import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;

import java.time.LocalDate;

public record HousingNoticeSummary(
        Long id,
        String title,
        String supplyType,
        String institution,
        TargetType targetType,
        LocalDate beginDate,
        LocalDate endDate
) {
    public static HousingNoticeSummary from(HousingNotice notice) {
        return new HousingNoticeSummary(
                notice.getId(),
                notice.getPblancNm(),
                notice.getSuplyTyNm(),
                notice.getSuplyInsttNm(),
                notice.getTargetType(),
                notice.getBeginDe(),
                notice.getEndDe()
        );
    }
}
