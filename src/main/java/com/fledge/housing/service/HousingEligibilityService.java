package com.fledge.housing.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.housing.domain.HousingEligibilityProfile;
import com.fledge.housing.domain.HousingEligibilityRuleType;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.dto.HousingEligibilityResponse;
import com.fledge.housing.repository.HousingEligibilityProfileRepository;
import com.fledge.member.domain.Member;
import com.fledge.member.domain.MemberRole;
import com.fledge.member.domain.ProtectionStatus;
import com.fledge.member.domain.MemberSurvey;
import com.fledge.member.repository.MemberRepository;
import com.fledge.member.repository.MemberSurveyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.fledge.housing.domain.HousingEligibilityStatus.*;

@Service
@Transactional(readOnly = true)
public class HousingEligibilityService {
    private final MemberRepository members;
    private final HousingEligibilityProfileRepository profiles;
    private final MemberSurveyRepository surveys;
    private final HousingEligibilityRuleResolver ruleResolver;
    private final LhYouthPurchaseEligibilityRule youthPurchaseRule;
    private final boolean demoEnabled;

    public HousingEligibilityService(MemberRepository members, HousingEligibilityProfileRepository profiles,
                                     MemberSurveyRepository surveys, HousingEligibilityRuleResolver ruleResolver,
                                     LhYouthPurchaseEligibilityRule youthPurchaseRule,
                                     @Value("${housing.eligibility.demo-enabled:false}") boolean demoEnabled) {
        this.members = members;
        this.profiles = profiles;
        this.surveys = surveys;
        this.ruleResolver = ruleResolver;
        this.youthPurchaseRule = youthPurchaseRule;
        this.demoEnabled = demoEnabled;
    }

    // 목록의 공고 수와 무관하게 회원 정보는 요청당 한 번만 읽는다.
    public Context load(Long memberId) {
        Member member = members.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return new Context(member, profiles.findById(memberId).orElse(null),
                surveys.findById(memberId).orElse(null),
                LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    public record Context(Member member, HousingEligibilityProfile profile, MemberSurvey survey, LocalDate date) {}

    public HousingEligibilityResponse evaluate(HousingNotice notice, Context context) {
        boolean lease = "SEED-SR-001".equals(notice.getPblancId());
        boolean purchase = "SEED-SR-002".equals(notice.getPblancId());
        HousingEligibilityRuleType resolvedRule = ruleResolver.resolve(notice);
        boolean demo = demoEnabled && resolvedRule == HousingEligibilityRuleType.SELF_RELIANCE_DEMO;
        if (notice.isSuperseded()) {
            return new HousingEligibilityResponse(NEEDS_CHECK, List.of("정정된 공고의 조건을 확인해주세요"),
                    List.of(), context.date(), demo, resolvedRule, null);
        }
        if (resolvedRule == HousingEligibilityRuleType.LH_YOUTH_PURCHASE) {
            return youthPurchaseRule.evaluate(notice, context.member(), context.profile(), context.survey(), context.date());
        }
        if (!demo) {
            return new HousingEligibilityResponse(NEEDS_CHECK, List.of("공고별 신청 조건 확인이 필요해요"),
                    List.of(), context.date(), false, HousingEligibilityRuleType.UNSUPPORTED, null);
        }
        if (context.member().getRole() != MemberRole.YOUTH) {
            return new HousingEligibilityResponse(NEEDS_CHECK, List.of("청년 회원의 자격 정보가 필요해요"),
                    List.of(), context.date(), true, resolvedRule, null);
        }

        // 2025년 자립준비청년 전세임대(마이홈 17490)와 매입임대(17446) 조건을
        // 날짜를 옮긴 데모에만 적용한다. 전세임대의 혼인 제한을 매입임대에 적용하지 않는다.
        // https://www.myhome.go.kr/hws/portal/sch/selectRsdtRcritNtcDetailView.do?pblancId=17490
        // https://m.myhome.go.kr/hws/portal/sch/selectRsdtRcritNtcDetailView.do?pblancId=17446
        List<String> failures = new ArrayList<>();
        List<String> checks = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        HousingEligibilityProfile profile = context.profile();
        if (profile == null) {
            missing.add("isHomeless");
            if (lease) missing.add("isMarried");
            checks.add("무주택·혼인 정보를 입력해주세요");
        } else {
            if (!profile.isHomeless()) failures.add("본인 무주택 조건을 충족하지 않아요");
            if (lease && profile.isMarried()) failures.add("혼인 중이 아닌 신청자만 지원할 수 있어요");
        }
        Member member = context.member();
        if (member.getProtectionStatus() == ProtectionStatus.ENDED) {
            LocalDate ended = member.getProtectionEndDate();
            if (ended == null || ended.isAfter(context.date())) {
                missing.add("protectionEndDate");
                checks.add("보호종료일을 확인해주세요");
            } else if (context.date().isAfter(ended.plusYears(5))) {
                failures.add("보호종료 후 5년 이내 조건을 충족하지 않아요");
            }
        } else {
            missing.add("protectionStatus");
            checks.add("보호연장 또는 보호종료 예정 대상인지 추가 확인이 필요해요");
        }
        if (member.getProtectionType() == null) {
            missing.add("protectionType");
            checks.add("시설 퇴소 또는 가정위탁 보호종료 여부를 확인해주세요");
        }
        if (!failures.isEmpty()) {
            return new HousingEligibilityResponse(NO_MATCH, List.copyOf(failures), List.copyOf(missing),
                    context.date(), true, resolvedRule, null);
        }
        if (!checks.isEmpty()) {
            return new HousingEligibilityResponse(NEEDS_CHECK, List.copyOf(checks), List.copyOf(missing),
                    context.date(), true, resolvedRule, null);
        }
        return new HousingEligibilityResponse(MATCH, List.of("입력한 정보가 데모 공고의 신청 조건을 충족해요"),
                List.of(), context.date(), true, resolvedRule, null);
    }
}
