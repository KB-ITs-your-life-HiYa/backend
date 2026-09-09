package com.fledge.housing.service;

import com.fledge.benefit.domain.Subsidy;
import com.fledge.benefit.repository.SubsidyRepository;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;
import com.fledge.housing.dto.HousingNoticeDetailResponse;
import com.fledge.housing.dto.HousingNoticeUnitResponse;
import com.fledge.housing.dto.RelatedNoticeSummary;
import com.fledge.housing.repository.HousingNoticeRepository;
import com.fledge.housing.repository.HousingNoticeUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousingNoticeService {

    private final HousingNoticeRepository noticeRepository;
    private final HousingNoticeUnitRepository unitRepository;
    private final HousingEligibilityService eligibilityService;
    private final SubsidyRepository subsidyRepository;

    public HousingNoticeDetailResponse findDetail(Long noticeId, Long memberId) {
        HousingNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ApiException(ErrorCode.HOUSING_NOTICE_NOT_FOUND));

        List<HousingNoticeUnitResponse> units = unitRepository.findByNoticeId(noticeId).stream()
                .sorted(Comparator.comparing(u -> u.getHouseSn() == null ? Integer.MAX_VALUE : u.getHouseSn()))
                .map(HousingNoticeUnitResponse::from)
                .toList();

        return HousingNoticeDetailResponse.from(notice, units,
                eligibilityService.evaluate(notice, eligibilityService.load(memberId)));
    }

    // 지원금 상세 화면의 "지금 접수 중인 관련 공고" 카드용. 지원금 이름에 공급유형(전세임대 등)이
    // 그대로 들어있는, 자립준비청년 대상·접수중인 공고만 관련 있다고 본다
    public List<RelatedNoticeSummary> findRelatedToSubsidy(Long subsidyId) {
        Subsidy subsidy = subsidyRepository.findById(subsidyId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원금을 찾을 수 없습니다"));

        LocalDate today = LocalDate.now();
        return noticeRepository.findByTargetTypeAndSupersededFalse(TargetType.SELF_RELIANCE).stream()
                .filter(n -> n.getEndDe() == null || !n.getEndDe().isBefore(today))
                .filter(n -> n.getSuplyTyNm() != null && subsidy.getName().contains(n.getSuplyTyNm()))
                .map(RelatedNoticeSummary::from)
                .toList();
    }
}
