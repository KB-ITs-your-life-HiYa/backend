package com.fledge.care.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.ingest.*;
import com.fledge.care.dto.CareDto.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CarePolicyServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final CareService care = mock(CareService.class);
    final YouthCenterRawClient youth = mock(YouthCenterRawClient.class);
    final CarePolicyService policies = new CarePolicyService(care, youth, mapper);
    final LocalDate day = LocalDate.of(2026, 9, 26);

    RawSubsidy row(String id, String name, String period) {
        return new RawSubsidy(id, "{\"plcyNo\":\"" + id + "\",\"plcyNm\":\"" + name
                + "\",\"plcySprtCn\":\"지원 내용\",\"aplyYmd\":\"" + period + "\",\"sprvsnInstCdNm\":\"고용노동부\"}");
    }

    @Test void actualResponseFieldsMapAndExpiredPoliciesAreExcluded() throws Exception {
        var card = CarePolicyService.toCard(mapper.readTree(row("2026001", "일경험", "20260101 ~ 20261231").rawPayload()), true, day);
        assertThat(card.name()).isEqualTo("일경험");
        assertThat(card.support()).isEqualTo("지원 내용");
        assertThat(card.detailUrl()).endsWith("/2026001");
        assertThat(card.category()).isEqualTo("EMPLOYMENT");
        assertThat(CarePolicyService.toCard(mapper.readTree(row("2", "마감", "20260807 ~ 20260813").rawPayload()), true, day)).isNull();
        assertThat(CarePolicyService.toCard(mapper.readTree(row("../../invalid", "잘못된 식별자", "").rawPayload()), false, day)).isNull();
    }

    @Test void employmentUsesFixedKeywordsDeduplicatesAndLimitsTwo() {
        when(care.policyContext(2L, 1L, 1L)).thenReturn(new PolicyContext(1L, "INCOME_MISSING", day, false));
        var rows = List.of(row("1", "일경험 A", ""), row("2", "일경험 B", ""), row("3", "일경험 C", ""));
        when(youth.fetchList(anyString(), eq(1), eq(100))).thenReturn(rows);
        policies.load(2L, 1L, 1L);
        verify(youth).fetchList("일경험", 1, 100);
        verify(youth).fetchList("국민취업지원", 1, 100);
        verify(care).savePolicies(eq(2L), eq(1L), eq(1L), argThat(p -> p.status().equals("READY")
                && p.cards().size() == 2 && p.cards().getFirst().id().equals("3")));
    }

    @Test void financeUsesFixedKeywordsAndEmptyResultRemainsRetryable() {
        when(care.policyContext(2L, 1L, 1L)).thenReturn(new PolicyContext(1L, "MISSED_SAVING", day, false));
        when(youth.fetchList(anyString(), eq(1), eq(100))).thenReturn(List.of());
        policies.load(2L, 1L, 1L);
        verify(youth).fetchList("생활비", 1, 100);
        verify(youth).fetchList("햇살론", 1, 100);
        verify(care).savePolicies(2L, 1L, 1L, new Policies("EMPTY", List.of()));
    }

    @Test void upstreamErrorIsStoredWithoutLeakingExceptionOrRepeatingCounseling() {
        when(care.policyContext(2L, 1L, 1L)).thenReturn(new PolicyContext(1L, "MISSED_PAYMENT", day, false));
        when(youth.fetchList(anyString(), eq(1), eq(100))).thenThrow(new IllegalStateException("key must not leak"));
        policies.load(2L, 1L, 1L);
        verify(care).savePolicies(2L, 1L, 1L, new Policies("ERROR", List.of()));
        verify(care, never()).respond(anyLong(), anyLong(), any());
    }

    @Test void successfulSnapshotSkipsExternalApi() {
        when(care.policyContext(2L, 1L, 1L)).thenReturn(new PolicyContext(1L, "MISSED_SAVING", day, true));
        policies.load(2L, 1L, 1L);
        verifyNoInteractions(youth);
        verify(care).summary(2L);
    }

    @Test void absentApiKeyDoesNotRequireNetworkAndConsentMustBeTrue() {
        var client = new YouthCenterRawClient();
        ReflectionTestUtils.setField(client, "apiKey", "");
        assertThatThrownBy(() -> client.fetchList("생활비", 1, 1)).isInstanceOf(IllegalStateException.class);
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new ReferralConsent(false))).isNotEmpty();
            assertThat(validator.validate(new ReferralConsent(null))).isNotEmpty();
            assertThat(validator.validate(new ReferralConsent(true))).isEmpty();
        }
    }
    @Test void duplicateSupportRegisteredByDifferentInstitutionsAppearsOnlyOnce() {
        when(care.policyContext(2L, 1L, 1L)).thenReturn(new PolicyContext(1L, "MISSED_SAVING", day, false));
        String support = "대학생과 청년층의 금융애로를 해소하고 제도권 금융으로 안착하도록 지원";
        var a = row("1", "햇살론유스", "");
        var b = row("2", "청년 햇살론유스 운영", "");
        when(youth.fetchList(anyString(), eq(1), eq(100))).thenReturn(List.of(
                new RawSubsidy(a.externalId(), a.rawPayload().replace("지원 내용", support)),
                new RawSubsidy(b.externalId(), b.rawPayload().replace("지원 내용", support))));
        policies.load(2L, 1L, 1L);
        verify(care).savePolicies(eq(2L), eq(1L), eq(1L), argThat(p -> p.cards().size() == 1));
    }
    @Test void samePolicyNameKeepsLatestEntry() {
        when(care.policyContext(2L, 1L, 1L)).thenReturn(new PolicyContext(1L, "INCOME_MISSING", day, false));
        when(youth.fetchList(anyString(), eq(1), eq(100))).thenReturn(List.of(
                row("2025", "국민취업지원제도", ""), row("2026", "국민취업지원제도", "")));
        policies.load(2L, 1L, 1L);
        verify(care).savePolicies(eq(2L), eq(1L), eq(1L), argThat(p -> p.cards().size() == 1 && p.cards().getFirst().id().equals("2026")));
    }
}
