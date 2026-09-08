package com.fledge.housing.service;

import com.fledge.housing.domain.HousingEligibilityProfile;
import com.fledge.housing.domain.HousingEligibilityRuleType;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.YouthPurchasePriorityBasis;
import com.fledge.housing.dto.HousingEligibilityResponse;
import com.fledge.member.domain.Member;
import com.fledge.member.domain.MemberRole;
import com.fledge.member.domain.MemberSurvey;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import static com.fledge.housing.domain.HousingEligibilityStatus.MATCH;
import static com.fledge.housing.domain.HousingEligibilityStatus.NEEDS_CHECK;
import static com.fledge.housing.domain.HousingEligibilityStatus.NO_MATCH;

@Component
public class LhYouthPurchaseEligibilityRule {
    private static final HousingEligibilityRuleType RULE_TYPE = HousingEligibilityRuleType.LH_YOUTH_PURCHASE;

    public HousingEligibilityResponse evaluate(HousingNotice notice, Member member,
                                               HousingEligibilityProfile profile, MemberSurvey survey,
                                               LocalDate evaluatedOn) {
        if (member.getRole() != MemberRole.YOUTH) {
            return result(NEEDS_CHECK, List.of("청년 회원의 자격 정보가 필요해요"), List.of(), evaluatedOn, null);
        }

        List<String> failures = new ArrayList<>();
        List<String> checks = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        evaluateHousingProfile(profile, failures, checks, missing);
        evaluateYouthRequirement(notice, member, survey, failures, checks, missing);

        if (!failures.isEmpty()) {
            return result(NO_MATCH, failures, missing, evaluatedOn, null);
        }

        YouthPurchasePriorityBasis basis = profile == null ? null : profile.getYouthPurchasePriorityBasis();
        if (basis == null) {
            missing.add("youthPurchasePriorityBasis");
            checks.add("청년 매입임대 1순위 해당 여부를 입력해주세요");
        } else if (basis == YouthPurchasePriorityBasis.NONE) {
            checks.add("1순위에는 해당하지 않으며 2·3순위 소득 및 자산 기준 확인이 필요해요");
        }

        if (!checks.isEmpty()) {
            return result(NEEDS_CHECK, checks, missing, evaluatedOn, null);
        }

        return result(MATCH, List.of(
                "무주택·미혼 청년 공통 조건을 충족해요",
                priorityReason(basis)), List.of(), evaluatedOn, 1);
    }

    private void evaluateHousingProfile(HousingEligibilityProfile profile, List<String> failures,
                                        List<String> checks, List<String> missing) {
        if (profile == null) {
            missing.add("isHomeless");
            missing.add("isMarried");
            checks.add("무주택·혼인 정보를 입력해주세요");
            return;
        }
        if (!profile.isHomeless()) {
            failures.add("본인 무주택 조건을 충족하지 않아요");
        }
        if (profile.isMarried()) {
            failures.add("혼인 중이 아닌 청년만 신청할 수 있어요");
        }
    }

    private void evaluateYouthRequirement(HousingNotice notice, Member member, MemberSurvey survey,
                                          List<String> failures, List<String> checks, List<String> missing) {
        LocalDate announceDate = notice.getRcritPblancDe();
        LocalDate birthDate = member.getBirthDate();
        if (announceDate == null || birthDate == null || birthDate.isAfter(announceDate)) {
            missing.add(announceDate == null ? "announceDate" : "birthDate");
            checks.add("공고일 기준 나이를 확인해주세요");
            return;
        }

        int age = Period.between(birthDate, announceDate).getYears();
        if (age >= 19 && age <= 39) {
            return;
        }

        String employmentStatus = survey == null ? null : survey.getEmploymentStatus();
        if (employmentStatus == null) {
            missing.add("employmentStatus");
            checks.add("대학생 또는 취업준비생 여부를 입력해주세요");
        } else if (!employmentStatus.equals("STUDENT") && !employmentStatus.equals("JOB_SEEKER")) {
            failures.add("만 19~39세, 대학생 또는 취업준비생 조건을 충족하지 않아요");
        }
    }

    private String priorityReason(YouthPurchasePriorityBasis basis) {
        return switch (basis) {
            case BENEFIT_RECIPIENT -> "수급자 가구로 청년 매입임대 1순위에 해당할 가능성이 높아요";
            case SUPPORTED_SINGLE_PARENT -> "지원 대상 한부모가족으로 청년 매입임대 1순위에 해당할 가능성이 높아요";
            case NEAR_POVERTY -> "차상위계층 가구로 청년 매입임대 1순위에 해당할 가능성이 높아요";
            case NONE -> throw new IllegalArgumentException("1순위 근거가 없습니다");
        };
    }

    private HousingEligibilityResponse result(com.fledge.housing.domain.HousingEligibilityStatus status,
                                              List<String> reasons, List<String> missingFields,
                                              LocalDate evaluatedOn, Integer priority) {
        return new HousingEligibilityResponse(status, List.copyOf(reasons), List.copyOf(missingFields),
                evaluatedOn, false, RULE_TYPE, priority);
    }
}
