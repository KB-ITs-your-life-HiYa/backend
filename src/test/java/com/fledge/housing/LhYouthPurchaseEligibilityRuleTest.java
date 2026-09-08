package com.fledge.housing;

import com.fledge.housing.domain.HousingEligibilityProfile;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;
import com.fledge.housing.domain.YouthPurchasePriorityBasis;
import com.fledge.housing.service.LhYouthPurchaseEligibilityRule;
import com.fledge.member.domain.Member;
import com.fledge.member.domain.MemberRole;
import com.fledge.member.domain.MemberSurvey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.fledge.housing.domain.HousingEligibilityRuleType.LH_YOUTH_PURCHASE;
import static com.fledge.housing.domain.HousingEligibilityStatus.MATCH;
import static com.fledge.housing.domain.HousingEligibilityStatus.NEEDS_CHECK;
import static com.fledge.housing.domain.HousingEligibilityStatus.NO_MATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LhYouthPurchaseEligibilityRuleTest {
    private final LocalDate announceDate = LocalDate.of(2026, 9, 3);
    private final LocalDate evaluatedOn = LocalDate.of(2026, 9, 8);
    private final LhYouthPurchaseEligibilityRule rule = new LhYouthPurchaseEligibilityRule();
    private final Member member = mock(Member.class);

    @BeforeEach
    void member() {
        when(member.getRole()).thenReturn(MemberRole.YOUTH);
        when(member.getBirthDate()).thenReturn(LocalDate.of(2004, 9, 7));
    }

    @Test
    void 공통조건과_1순위_근거를_충족하면_match다() {
        var result = rule.evaluate(notice(), member,
                profile(true, false, YouthPurchasePriorityBasis.BENEFIT_RECIPIENT), null, evaluatedOn);

        assertThat(result.status()).isEqualTo(MATCH);
        assertThat(result.ruleType()).isEqualTo(LH_YOUTH_PURCHASE);
        assertThat(result.priority()).isEqualTo(1);
        assertThat(result.demo()).isFalse();
        assertThat(result.reasons()).contains("무주택·미혼 청년 공통 조건을 충족해요");
    }

    @Test
    void 일순위_근거가_없으면_이삼순위_확인필요다() {
        var result = rule.evaluate(notice(), member,
                profile(true, false, YouthPurchasePriorityBasis.NONE), null, evaluatedOn);

        assertThat(result.status()).isEqualTo(NEEDS_CHECK);
        assertThat(result.priority()).isNull();
        assertThat(result.reasons()).contains("1순위에는 해당하지 않으며 2·3순위 소득 및 자산 기준 확인이 필요해요");
    }

    @Test
    void 일순위_답변을_하지_않으면_누락항목을_알려준다() {
        var result = rule.evaluate(notice(), member, profile(true, false, null), null, evaluatedOn);

        assertThat(result.status()).isEqualTo(NEEDS_CHECK);
        assertThat(result.missingFields()).containsExactly("youthPurchasePriorityBasis");
    }

    @Test
    void 주택을_소유했거나_혼인중이면_no_match다() {
        assertThat(rule.evaluate(notice(), member,
                profile(false, false, YouthPurchasePriorityBasis.NEAR_POVERTY), null, evaluatedOn).status())
                .isEqualTo(NO_MATCH);
        assertThat(rule.evaluate(notice(), member,
                profile(true, true, YouthPurchasePriorityBasis.NEAR_POVERTY), null, evaluatedOn).status())
                .isEqualTo(NO_MATCH);
    }

    @Test
    void 나이는_오늘이_아닌_공고일을_기준으로_판정한다() {
        MemberSurvey survey = new MemberSurvey(1L);
        survey.setEmploymentStatus("UNEMPLOYED");
        when(member.getBirthDate()).thenReturn(announceDate.minusYears(40));
        assertThat(rule.evaluate(notice(), member,
                profile(true, false, YouthPurchasePriorityBasis.NEAR_POVERTY), survey, evaluatedOn).status())
                .isEqualTo(NO_MATCH);

        when(member.getBirthDate()).thenReturn(announceDate.minusYears(40).plusDays(1));
        assertThat(rule.evaluate(notice(), member,
                profile(true, false, YouthPurchasePriorityBasis.NEAR_POVERTY), null, evaluatedOn).status())
                .isEqualTo(MATCH);
    }

    @Test
    void 연령범위_밖이어도_대학생이나_취업준비생이면_신청대상이다() {
        when(member.getBirthDate()).thenReturn(LocalDate.of(1980, 1, 1));
        MemberSurvey survey = new MemberSurvey(1L);
        survey.setEmploymentStatus("STUDENT");
        assertThat(rule.evaluate(notice(), member,
                profile(true, false, YouthPurchasePriorityBasis.SUPPORTED_SINGLE_PARENT), survey, evaluatedOn).status())
                .isEqualTo(MATCH);

        survey.setEmploymentStatus("UNEMPLOYED");
        assertThat(rule.evaluate(notice(), member,
                profile(true, false, YouthPurchasePriorityBasis.SUPPORTED_SINGLE_PARENT), survey, evaluatedOn).status())
                .isEqualTo(NO_MATCH);
    }

    private HousingNotice notice() {
        HousingNotice notice = new HousingNotice("21147");
        notice.update("청년 매입임대주택 모집", "LH", "다가구주택", "매입임대",
                TargetType.YOUTH, "공고중", null, announceDate, null, null, null, null, null, null);
        return notice;
    }

    private HousingEligibilityProfile profile(boolean homeless, boolean married,
                                              YouthPurchasePriorityBasis basis) {
        HousingEligibilityProfile profile = new HousingEligibilityProfile(1L);
        profile.update(homeless, married);
        if (basis != null) {
            profile.updateYouthPurchasePriorityBasis(basis);
        }
        return profile;
    }
}
