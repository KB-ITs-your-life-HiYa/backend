package com.fledge.benefit.service;

import com.fledge.benefit.domain.Subsidy;
import com.fledge.benefit.domain.SubsidyBenefit;
import com.fledge.benefit.domain.SubsidyRegion;
import com.fledge.benefit.dto.CategoryMatchResponse;
import com.fledge.benefit.dto.MatchCondition;
import com.fledge.benefit.dto.MatchStatus;
import com.fledge.benefit.dto.SubsidyMatchResponse;
import com.fledge.benefit.repository.SubsidyBenefitRepository;
import com.fledge.benefit.repository.SubsidyRegionRepository;
import com.fledge.benefit.repository.SubsidyRepository;
import com.fledge.member.domain.Member;
import com.fledge.member.domain.MemberSurvey;
import com.fledge.member.domain.MemberSurveyTag;
import com.fledge.member.domain.ProtectionStatus;
import com.fledge.member.repository.MemberRepository;
import com.fledge.member.repository.MemberSurveyRepository;
import com.fledge.member.repository.MemberSurveyTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BenefitMatchingService {

    private static final List<String> CATEGORY_ORDER =
            List.of("생활안정", "주거자립", "교육", "취업", "금융", "보건의료", "보호돌봄");

    // member_survey_tag 와 같은 어휘. "대상 특성" 조건에 사람이 읽을 수 있는 이름을 붙이는 용도
    private static final Map<String, String> TAG_LABELS = Map.of(
            "SINGLE_PARENT", "한부모",
            "MULTICULTURAL", "다문화",
            "DISABILITY", "장애",
            "MULTI_CHILD", "다자녀",
            "SEVERE_ILLNESS", "중증질환",
            "NORTH_KOREAN_DEFECTOR", "북한이탈",
            "GRANDPARENT_FAMILY", "조손가정"
    );

    private final MemberRepository memberRepository;
    private final MemberSurveyRepository memberSurveyRepository;
    private final MemberSurveyTagRepository memberSurveyTagRepository;
    private final SubsidyRepository subsidyRepository;
    private final SubsidyBenefitRepository subsidyBenefitRepository;
    private final SubsidyRegionRepository subsidyRegionRepository;

    public List<CategoryMatchResponse> getMatches(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        MemberSurvey survey = memberSurveyRepository.findById(memberId).orElse(null);
        Set<String> tags = memberSurveyTagRepository.findByMemberId(memberId).stream()
                .map(MemberSurveyTag::getTag)
                .collect(Collectors.toSet());

        List<SubsidyMatchResponse> matches = subsidyRepository.findAll().stream()
                .map(s -> evaluate(s, member, survey, tags))
                .flatMap(Optional::stream)
                .toList();

        Map<String, List<SubsidyMatchResponse>> grouped = matches.stream()
                .collect(Collectors.groupingBy(SubsidyMatchResponse::category));

        return CATEGORY_ORDER.stream()
                .map(cat -> new CategoryMatchResponse(cat,
                        grouped.getOrDefault(cat, List.of()).stream()
                                .sorted(Comparator.comparingLong(SubsidyMatchResponse::needsReviewCount))
                                .toList()))
                .filter(c -> !c.items().isEmpty())
                .toList();
    }

    private Optional<SubsidyMatchResponse> evaluate(Subsidy s, Member member, MemberSurvey survey, Set<String> tags) {
        List<MatchCondition> conditions = new ArrayList<>();

        if (s.getMinAge() != null || s.getMaxAge() != null) {
            int age = member.getAge();
            boolean ok = (s.getMinAge() == null || age >= s.getMinAge())
                    && (s.getMaxAge() == null || age <= s.getMaxAge());
            if (!ok) return Optional.empty();
            conditions.add(new MatchCondition("나이 조건", MatchStatus.MET));
        }

        if (s.getProtectionStatusRequired() != null) {
            boolean requiresEnded = s.getProtectionStatusRequired().contains("종료");
            boolean memberEnded = member.getProtectionStatus() == ProtectionStatus.ENDED;
            if (requiresEnded != memberEnded) return Optional.empty();
            conditions.add(new MatchCondition("보호종료 요건", MatchStatus.MET));
        }

        if (s.getMinYearsAfterEnd() != null || s.getMaxYearsAfterEnd() != null) {
            if (member.getProtectionStatus() != ProtectionStatus.ENDED || member.getProtectionEndDate() == null) {
                conditions.add(new MatchCondition("경과기간 요건", MatchStatus.NEEDS_REVIEW));
            } else {
                int years = Period.between(member.getProtectionEndDate(), LocalDate.now()).getYears();
                boolean ok = (s.getMinYearsAfterEnd() == null || years >= s.getMinYearsAfterEnd().intValue())
                        && (s.getMaxYearsAfterEnd() == null || years <= s.getMaxYearsAfterEnd().intValue());
                if (!ok) return Optional.empty();
                conditions.add(new MatchCondition("경과기간 요건", MatchStatus.MET));
            }
        }

        if (s.getIncomePctMax() != null) {
            Integer bracket = survey == null ? null : survey.getIncomePctBracket();
            if (bracket == null) {
                conditions.add(new MatchCondition("소득 기준", MatchStatus.NEEDS_REVIEW));
            } else if (bracket > s.getIncomePctMax()) {
                return Optional.empty();
            } else {
                conditions.add(new MatchCondition("소득 기준", MatchStatus.MET));
            }
        }

        if (s.getIncomeAmtMin() != null || s.getIncomeAmtMax() != null) {
            conditions.add(new MatchCondition("소득 기준(금액)", MatchStatus.NEEDS_REVIEW));
        }

        if (s.getTargetHousehold() != null && !s.getTargetHousehold().isEmpty()) {
            boolean hasAny = s.getTargetHousehold().stream().anyMatch(tags::contains);
            String targetLabel = s.getTargetHousehold().stream()
                    .map(t -> TAG_LABELS.getOrDefault(t, t))
                    .collect(Collectors.joining("·"));
            conditions.add(new MatchCondition("대상 특성(" + targetLabel + ")", hasAny ? MatchStatus.MET : MatchStatus.NEEDS_REVIEW));
        }

        List<SubsidyRegion> regions = subsidyRegionRepository.findBySubsidy_Id(s.getId());
        if (!regions.isEmpty()) {
            boolean regionOk = regions.stream().anyMatch(r ->
                    r.getSigunguCode() != null
                            ? r.getSigunguCode().equals(member.getRegionSigunguCode())
                            : r.getSidoCode().equals(member.getRegionCode()));
            if (!regionOk) return Optional.empty();
            conditions.add(new MatchCondition("지역 조건", MatchStatus.MET));
        }

        long needsReviewCount = conditions.stream().filter(c -> c.status() == MatchStatus.NEEDS_REVIEW).count();
        List<SubsidyBenefit> benefits = subsidyBenefitRepository.findBySubsidy_Id(s.getId());

        return Optional.of(new SubsidyMatchResponse(
                s.getId(), s.getName(), s.getSummary(), s.getOrgName(), s.getCategory(),
                s.getApplyMethod(), s.getApplyDeadlineRaw(), s.getApplyDeadlineDate(), s.getDetailUrl(),
                benefits.stream().map(SubsidyMatchResponse.BenefitItem::from).toList(),
                conditions, needsReviewCount
        ));
    }
}