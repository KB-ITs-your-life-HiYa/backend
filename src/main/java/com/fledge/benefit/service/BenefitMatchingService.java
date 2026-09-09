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
import com.fledge.member.domain.MemberSubsidy;
import com.fledge.member.domain.MemberSurvey;
import com.fledge.member.domain.MemberSurveyTag;
import com.fledge.member.domain.ProtectionStatus;
import com.fledge.member.repository.MemberRepository;
import com.fledge.member.repository.MemberSubsidyRepository;
import com.fledge.member.repository.MemberSurveyRepository;
import com.fledge.member.repository.MemberSurveyTagRepository;
import com.fledge.region.service.RegionNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    // 카탈로그 초기 수집 때 "자립준비청년" 키워드로 찾은 항목은 대부분 이름에 이 단어들이
    // 들어있다. 이후 청년도약계좌·청년월세 같은 일반 청년 정책을 추가로 모았는데, 그건
    // 이 단어가 안 들어가 있어서 정확히 구분된다(원본 카탈로그 55건 중 47건이 이 패턴에
    // 걸리고, 새로 모은 일반 청년 정책은 하나도 잘못 걸리지 않는 것을 직접 확인했다)
    private static final List<String> SELF_RELIANCE_KEYWORDS =
            List.of("자립준비청년", "보호종료", "보호대상아동");

    // 카테고리 안 정렬: 자립준비청년 전용 정책을 먼저, 그다음은 충족 배지(초록)가 많은 순.
    // 조건이 아예 없는 지원금(정보 없음)이 needsReviewCount 만으로는 "확인 필요 0개"라
    // 충족 배지가 여러 개인 지원금보다 위로 올라가버리는 문제가 있어서, 충족 개수를
    // 먼저 비교하고 확인 필요 개수는 동점자 tiebreaker 로만 쓴다
    private static final Comparator<SubsidyMatchResponse> CATEGORY_ITEM_ORDER =
            Comparator.<SubsidyMatchResponse>comparingInt(m -> isSelfRelianceSpecific(m.name()) ? 0 : 1)
                    .thenComparing(Comparator.comparingInt(BenefitMatchingService::metConditionCount).reversed())
                    .thenComparingLong(SubsidyMatchResponse::needsReviewCount);

    private static boolean isSelfRelianceSpecific(String name) {
        return SELF_RELIANCE_KEYWORDS.stream().anyMatch(name::contains);
    }

    // income_pct_max 같은 구조화된 소득 필드가 없는 지원금 중 이름에 "수급자"/"차상위"가
    // 명시된 경우, 그 자체가 대상 조건이다(예: "자활근로(기초, 차상위)"). 다만 "초과"가
    // 같이 들어가면(예: "차상위 이하, 초과 통합") 수급자가 아니어도 되는 트랙이 섞여있는
    // 것이라 걸지 않는다
    private static boolean requiresBenefitRecipient(String name) {
        boolean mentionsRecipient = name.contains("수급자") || name.contains("차상위");
        boolean mixedTier = name.contains("초과");
        return mentionsRecipient && !mixedTier;
    }

    // 충족 배지(초록) 개수. 조건 자체가 하나도 없는 지원금(배지가 아예 안 뜨는 것)은
    // 0으로 계산되어 충족 배지가 있는 지원금들보다 아래로 내려간다
    private static int metConditionCount(SubsidyMatchResponse m) {
        return m.conditions().size() - (int) m.needsReviewCount();
    }

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
    private final RegionNameResolver regionNameResolver;
    private final MemberSubsidyRepository memberSubsidyRepository;

    public List<CategoryMatchResponse> getMatches(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        MemberSurvey survey = memberSurveyRepository.findById(memberId).orElse(null);
        Set<String> tags = memberSurveyTagRepository.findByMemberId(memberId).stream()
                .map(MemberSurveyTag::getTag)
                .collect(Collectors.toSet());
        // 이미 받고 있는 지원금은 추천 목록에서 뺀다. 같은 지원금이 지역별로 이름만 같은 채
        // 여러 건 등록돼 있는 경우가 많아(예: 자립수당), ID뿐 아니라 이름이 같은 것도 함께 뺀다
        List<Subsidy> receivingSubsidies = subsidyRepository.findAllById(
                memberSubsidyRepository.findByMemberId(memberId).stream()
                        .map(MemberSubsidy::getSubsidyId)
                        .toList());
        Set<String> receivingNames = receivingSubsidies.stream()
                .map(Subsidy::getName)
                .collect(Collectors.toSet());

        List<SubsidyMatchResponse> evaluated = subsidyRepository.findAll().stream()
                .filter(s -> !receivingNames.contains(s.getName()))
                .map(s -> evaluate(s, member, survey, tags))
                .flatMap(Optional::stream)
                .toList();

        // 여기까지 살아남은 건 전부 이 회원 지역 조건을 이미 통과한 것들이라, 이름이 같으면
        // (표기만 다른 중복 수집 포함) 지역을 어떻게 나눠서 등록했든 하나로 묶어도 된다 —
        // 어차피 다 이 회원한테 적용되는 것들이라 지역별로 따로 보여줄 이유가 없다
        List<SubsidyMatchResponse> matches = dedupeByName(evaluated);

        Map<String, List<SubsidyMatchResponse>> grouped = matches.stream()
                .collect(Collectors.groupingBy(SubsidyMatchResponse::category));

        return CATEGORY_ORDER.stream()
                .map(cat -> new CategoryMatchResponse(cat,
                        grouped.getOrDefault(cat, List.of()).stream()
                                .sorted(CATEGORY_ITEM_ORDER)
                                .toList()))
                .filter(c -> !c.items().isEmpty())
                .toList();
    }

    private List<SubsidyMatchResponse> dedupeByName(List<SubsidyMatchResponse> matches) {
        Map<String, SubsidyMatchResponse> deduped = new LinkedHashMap<>();
        for (SubsidyMatchResponse m : matches) {
            String key = SubsidyNameNormalizer.normalize(m.name());
            SubsidyMatchResponse existing = deduped.get(key);
            if (existing == null || isRicher(m, existing)) {
                deduped.put(key, m);
            }
        }
        return List.copyOf(deduped.values());
    }

    // 기관명이 있는 쪽을 우선하고, 그다음은 확인 필요 조건이 더 적은(더 확실하게 충족된) 쪽을 우선한다
    private boolean isRicher(SubsidyMatchResponse candidate, SubsidyMatchResponse existing) {
        if (existing.orgName() == null && candidate.orgName() != null) return true;
        if (existing.orgName() != null && candidate.orgName() == null) return false;
        return candidate.needsReviewCount() < existing.needsReviewCount();
    }

    // SubsidyService.regionLabelOf와 같은 규칙: 지역 정보가 없으면 null(전국 단위)
    private String regionLabelOf(List<SubsidyRegion> regions) {
        if (regions.isEmpty()) return null;
        return regions.stream()
                .map(r -> regionNameResolver.resolve(r.getSidoCode(), r.getSigunguCode()))
                .filter(name -> name != null)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new),
                        names -> names.isEmpty() ? null : String.join("·", names)));
    }

    // 지역이 3곳을 넘어가면(예: 구·군 단위로 잔뜩 걸린 경우) 배지에 다 나열하면 넘쳐서,
    // 그때는 어느 지역인지 굳이 안 붙이고 "지역 조건"만 보여준다
    private String regionConditionLabel(List<SubsidyRegion> regions) {
        String label = regionLabelOf(regions);
        if (label == null) return "지역 조건";
        return label.split("·").length <= 2 ? "지역 조건(" + label + ")" : "지역 조건";
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

        if (requiresBenefitRecipient(s.getName())) {
            boolean isRecipient = survey != null && Boolean.TRUE.equals(survey.getIsBenefitRecipient());
            if (!isRecipient) return Optional.empty();
            conditions.add(new MatchCondition("기초생활수급자 요건", MatchStatus.MET));
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
            conditions.add(new MatchCondition(regionConditionLabel(regions), MatchStatus.MET));
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