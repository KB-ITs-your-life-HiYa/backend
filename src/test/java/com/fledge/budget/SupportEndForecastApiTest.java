package com.fledge.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupportEndForecastApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String FORECAST = "/api/v1/members/me/support-end-forecast";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private String loginAndGetToken(String email) throws Exception {
        String response = mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"demo1234"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void 종료까지_1년_넘게_남았으면_노출되지_않는다() throws Exception {
        // demo1: D-1275. 노출 조건(365일 이내) 밖이라 계산 없이 플래그만 내려온다
        mvc.perform(get(FORECAST).header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.forecast").value(Matchers.nullValue()));
    }

    @Test
    void 종료가_임박하면_노출되고_계산값을_내려준다() throws Exception {
        // demo2: D-180. 시드 데이터는 7·8월만 있고(9월은 진행 중이라 제외) 정확한 고정값으로 검증 가능.
        // 서버 "오늘"이 2026년 9월인 동안만 유효 (완결된 달이 7·8월이 되는 전제). 10월로 넘어가면 8·9월로 바뀐다.
        String response = mvc.perform(get(FORECAST).header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.forecast.monthsUsedForAverage").value(2))
                .andExpect(jsonPath("$.data.forecast.dataAvailable").value(true))
                .andExpect(jsonPath("$.data.forecast.currentSavingsTotal").value(3580000))
                .andExpect(jsonPath("$.data.forecast.incomeExcludingAllowance").value(381840))
                .andExpect(jsonPath("$.data.forecast.averageExpense").value(799950))
                .andExpect(jsonPath("$.data.forecast.monthlyShortfall").value(418110))
                .andExpect(jsonPath("$.data.forecast.savingsRunwayMonths").value(8))
                .andExpect(jsonPath("$.data.forecast.reduction.categories.length()").value(4))
                .andExpect(jsonPath("$.data.forecast.reduction.totalMonthlySavings").value(58455))
                .andExpect(jsonPath("$.data.forecast.reduction.totalSavingsByEnd").value(58455 * 6))
                .andExpect(jsonPath("$.data.forecast.reduction.improvedRunwayMonths").value(9))
                .andReturn().getResponse().getContentAsString();

        // HOUSING_UTILITY, SAVINGS는 절감 대상이 아니므로 응답에 없어야 한다
        JsonNode categories = objectMapper.readTree(response).path("data").path("forecast").path("reduction").path("categories");
        assertThat(categories).hasSize(4);
        for (JsonNode c : categories) {
            String category = c.path("category").asText();
            assertThat(category).isNotEqualTo("HOUSING_UTILITY").isNotEqualTo("SAVINGS");
            // 10% 절감 = 절감액이 평균의 10%(반올림 오차 1원 이내)여야 한다
            long average = c.path("averageAmount").asLong();
            long savings = c.path("monthlySavings").asLong();
            assertThat(Math.abs(savings - Math.round(average * 0.1))).isLessThanOrEqualTo(1);
        }
    }

    @Test
    void 개선된_기간은_기존_기간보다_짧지_않다() throws Exception {
        String response = mvc.perform(get(FORECAST).header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode forecast = objectMapper.readTree(response).path("data").path("forecast");
        long currentRunway = forecast.path("savingsRunwayMonths").asLong();
        long improvedRunway = forecast.path("reduction").path("improvedRunwayMonths").asLong();
        assertThat(improvedRunway).isGreaterThanOrEqualTo(currentRunway);
    }

    @Test
    void 토큰_없이_조회하면_401() throws Exception {
        mvc.perform(get(FORECAST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 다른_회원의_데이터는_섞이지_않는다() throws Exception {
        // demo1(노출 안 됨)과 demo2(노출됨)가 서로 다른 결과를 받는지로 격리를 확인한다
        String demo1Response = mvc.perform(get(FORECAST).header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andReturn().getResponse().getContentAsString();
        String demo2Response = mvc.perform(get(FORECAST).header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(demo1Response).path("data").path("eligible").asBoolean()).isFalse();
        assertThat(objectMapper.readTree(demo2Response).path("data").path("eligible").asBoolean()).isTrue();
    }
}