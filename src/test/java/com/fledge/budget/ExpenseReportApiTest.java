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
class ExpenseReportApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String SUMMARY = "/api/v1/members/me/expense-summary";
    private static final String REPORT = "/api/v1/members/me/expense-report";

    // demo1 시드 데이터 기준 이미 끝난 달(2026-08)의 고정값. 지난 달이라 "오늘" 과 무관하게 항상 같다
    private static final String CLOSED_MONTH = "2026-08";

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
    void 지출_요약을_조회한다() throws Exception {
        String response = mvc.perform(get(SUMMARY).header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        long current = data.path("currentMonthTotal").asLong();
        long last = data.path("lastMonthSamePeriodTotal").asLong();
        long diff = data.path("difference").asLong();

        // "오늘" 기준으로 매일 값이 바뀌는 API 라 정확한 금액 대신 관계만 검증한다
        assertThat(current).isGreaterThanOrEqualTo(0);
        assertThat(last).isGreaterThanOrEqualTo(0);
        assertThat(diff).isEqualTo(current - last);
    }

    @Test
    void 이미_끝난_달의_리포트를_조회한다() throws Exception {
        mvc.perform(get(REPORT).param("month", CLOSED_MONTH)
                        .header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(CLOSED_MONTH))
                .andExpect(jsonPath("$.data.summary.totalExpense").value(791600))
                .andExpect(jsonPath("$.data.summary.totalIncome").value(500000))
                .andExpect(jsonPath("$.data.monthlyTrend.months.length()").value(3))
                .andExpect(jsonPath("$.data.monthlyTrend.months[2].month").value(CLOSED_MONTH))
                .andExpect(jsonPath("$.data.categories.length()").value(6))
                .andExpect(jsonPath("$.data.categories[0].category").value("HOUSING_UTILITY"))
                .andExpect(jsonPath("$.data.categories[0].currentAmount").value(236700))
                .andExpect(jsonPath("$.data.categories[0].previousAmount").value(232100))
                .andExpect(jsonPath("$.data.categories[0].difference").value(4600))
                .andExpect(jsonPath("$.data.categories[0].budget").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.navigation.hasPrevious").value(true))
                .andExpect(jsonPath("$.data.navigation.hasNext").value(true))
                .andExpect(jsonPath("$.data.monthlyBudget").value(Matchers.nullValue()));
    }

    @Test
    void 잘못된_month이면_400() throws Exception {
        mvc.perform(get(REPORT).param("month", "nope")
                        .header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 토큰_없이_조회하면_401() throws Exception {
        mvc.perform(get(SUMMARY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 다른_회원의_지출은_섞이지_않는다() throws Exception {
        mvc.perform(get(REPORT).param("month", CLOSED_MONTH)
                        .header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.totalExpense").value(776300));
    }
}
