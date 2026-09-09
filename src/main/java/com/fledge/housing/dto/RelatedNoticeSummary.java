package com.fledge.housing.dto;

import com.fledge.housing.domain.HousingNotice;

// 지원금 상세 화면의 "지금 접수 중인 관련 공고" 카드에서 쓰는 최소 정보
public record RelatedNoticeSummary(
        Long id,
        String title
) {
    public static RelatedNoticeSummary from(HousingNotice notice) {
        return new RelatedNoticeSummary(notice.getId(), notice.getPblancNm());
    }
}
