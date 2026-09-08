package com.fledge.housing;

import com.fledge.housing.domain.HousingEligibilityProfile;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.service.HousingEligibilityService;
import com.fledge.member.domain.Member;
import com.fledge.member.domain.MemberRole;
import com.fledge.member.domain.ProtectionStatus;
import com.fledge.member.domain.ProtectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.fledge.housing.domain.HousingEligibilityStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HousingEligibilityServiceTest {
    private final LocalDate today = LocalDate.of(2026, 9, 8);
    private final Member member = mock(Member.class);
    private final HousingEligibilityService service = new HousingEligibilityService(null, null, true);

    @BeforeEach
    void member() {
        when(member.getRole()).thenReturn(MemberRole.YOUTH);
        when(member.getProtectionStatus()).thenReturn(ProtectionStatus.ENDED);
        when(member.getProtectionType()).thenReturn(ProtectionType.FOSTER_CARE);
        when(member.getProtectionEndDate()).thenReturn(today.minusYears(3));
    }

    private HousingEligibilityService.Context context(Boolean homeless, boolean married) {
        HousingEligibilityProfile profile = null;
        if (homeless != null) {
            profile = new HousingEligibilityProfile(1L);
            profile.update(homeless, married);
        }
        return new HousingEligibilityService.Context(member, profile, today);
    }

    @Test void 모든_필수조건을_충족하면_match() {
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(true, false)).status()).isEqualTo(MATCH);
    }

    @Test void 미입력은_확인필요() {
        var result = service.evaluate(new HousingNotice("SEED-SR-001"), context(null, false));
        assertThat(result.status()).isEqualTo(NEEDS_CHECK);
        assertThat(result.missingFields()).containsExactly("isHomeless", "isMarried");
    }

    @Test void 주택소유자는_미충족() {
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(false, false)).status()).isEqualTo(NO_MATCH);
    }

    @Test void 혼인조건은_전세임대에만_적용() {
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(true, true)).status()).isEqualTo(NO_MATCH);
        assertThat(service.evaluate(new HousingNotice("SEED-SR-002"), context(true, true)).status()).isEqualTo(MATCH);
    }

    @Test void 달력상_5년_당일은_충족하고_다음날부터_미충족() {
        when(member.getProtectionEndDate()).thenReturn(today.minusYears(5));
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(true, false)).status()).isEqualTo(MATCH);
        when(member.getProtectionEndDate()).thenReturn(today.minusYears(5).minusDays(1));
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(true, false)).status()).isEqualTo(NO_MATCH);
    }

    @Test void 보호중이거나_종료일이_미래이면_추가확인() {
        when(member.getProtectionStatus()).thenReturn(ProtectionStatus.IN_CARE);
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(true, false)).status()).isEqualTo(NEEDS_CHECK);
        when(member.getProtectionStatus()).thenReturn(ProtectionStatus.ENDED);
        when(member.getProtectionEndDate()).thenReturn(today.plusDays(1));
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(true, false)).status()).isEqualTo(NEEDS_CHECK);
    }

    @Test void 종료일_또는_보호유형_누락은_추가확인() {
        when(member.getProtectionEndDate()).thenReturn(null);
        when(member.getProtectionType()).thenReturn(null);
        var result = service.evaluate(new HousingNotice("SEED-SR-001"), context(true, false));
        assertThat(result.status()).isEqualTo(NEEDS_CHECK);
        assertThat(result.missingFields()).contains("protectionEndDate", "protectionType");
    }

    @Test void 미등록공고_정정공고_데모비활성은_자동충족시키지_않는다() {
        assertThat(service.evaluate(new HousingNotice("17490"), context(true, false)).status()).isEqualTo(NEEDS_CHECK);
        HousingNotice notice = new HousingNotice("SEED-SR-001");
        notice.markSuperseded();
        assertThat(service.evaluate(notice, context(true, false)).status()).isEqualTo(NEEDS_CHECK);
        var disabled = new HousingEligibilityService(null, null, false);
        var result = disabled.evaluate(new HousingNotice("SEED-SR-001"), context(true, false));
        assertThat(result.status()).isEqualTo(NEEDS_CHECK);
        assertThat(result.demo()).isFalse();
    }

    @Test void 상담사_자리채움_정보로_판정하지_않는다() {
        when(member.getRole()).thenReturn(MemberRole.COUNSELOR);
        assertThat(service.evaluate(new HousingNotice("SEED-SR-001"), context(true, false)).status()).isEqualTo(NEEDS_CHECK);
    }
}
