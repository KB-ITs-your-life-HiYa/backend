package com.fledge.housing;

import com.fledge.housing.domain.HousingEligibilityRuleType;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;
import com.fledge.housing.service.HousingEligibilityRuleResolver;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HousingEligibilityRuleResolverTest {
    private final HousingEligibilityRuleResolver resolver = new HousingEligibilityRuleResolver();

    @Test
    void lh_표준_청년_매입임대만_규칙을_선택한다() {
        assertThat(resolver.resolve(notice(
                "[강원지역본부] 26년 3차 청년 매입임대주택 예비자 모집공고", "LH", "매입임대", TargetType.YOUTH)))
                .isEqualTo(HousingEligibilityRuleType.LH_YOUTH_PURCHASE);
        assertThat(resolver.resolve(notice(
                "2026년 청년매입임대 입주자 모집공고", "LH", "매입임대", TargetType.YOUTH)))
                .isEqualTo(HousingEligibilityRuleType.LH_YOUTH_PURCHASE);
    }

    @Test
    void 이름이_비슷하지만_조건이_다른_공고는_제외한다() {
        for (String title : new String[]{
                "청년신혼부부매입임대리츠주택 상시모집",
                "기숙사형 청년 매입임대주택 모집",
                "칠곡군 천원주택(청년 매입임대주택) 모집",
                "청년 매입임대주택 입주자격완화 모집"}) {
            assertThat(resolver.resolve(notice(title, "LH", "매입임대", TargetType.YOUTH)))
                    .isEqualTo(HousingEligibilityRuleType.UNSUPPORTED);
        }
    }

    @Test
    void 공급기관_유형_대상이_다르면_제외한다() {
        String title = "청년 매입임대주택 모집";
        assertThat(resolver.resolve(notice(title, "경상북도개발공사", "매입임대", TargetType.YOUTH)))
                .isEqualTo(HousingEligibilityRuleType.UNSUPPORTED);
        assertThat(resolver.resolve(notice(title, "LH", "전세임대", TargetType.YOUTH)))
                .isEqualTo(HousingEligibilityRuleType.UNSUPPORTED);
        assertThat(resolver.resolve(notice(title, "LH", "매입임대", TargetType.GENERAL)))
                .isEqualTo(HousingEligibilityRuleType.UNSUPPORTED);
    }

    private HousingNotice notice(String title, String institution, String supplyType, TargetType targetType) {
        HousingNotice notice = new HousingNotice("actual");
        notice.update(title, institution, "다가구주택", supplyType, targetType, "공고중", null,
                LocalDate.of(2026, 9, 3), null, null, null, null, null, null);
        return notice;
    }
}
