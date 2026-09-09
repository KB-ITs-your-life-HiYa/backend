package com.fledge.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.budget.domain.FinancialTransaction;
import com.fledge.budget.repository.FinancialTransactionRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseReportApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String SUMMARY = "/api/v1/members/me/expense-summary";
    private static final String REPORT = "/api/v1/members/me/expense-report";
    private static final String BUDGET = "/api/v1/members/me/budget";

    // 리포트-예산 연동 테스트 전용 달. 다른 테스트의 고정값과 겹치지 않도록 둔다
    private static final String BUDGET_LINKED_MONTH = "2027-07";

    // demo1 시드 데이터 기준 이미 끝난 달(2026-08)의 고정값. 지난 달이라 "오늘" 과 무관하게 항상 같다
    private static final String CLOSED_MONTH = "2026-08";

    // 코칭(저축여력) 테스트 전용 달. 다른 테스트의 고정값과 겹치지 않도록 멀리 떨어뜨려 둔다.
    // 평균을 낼 이전 3개월(7~9월)에 30만원씩, 판정 대상 달(10월)에 20만원만 써서 -33% -> SURPLUS를 만든다.
    private static final String SURPLUS_MONTH = "2028-11";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired FinancialTransactionRepository transactionRepository;

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
    void 진행_중인_달의_리포트는_지출_요약과_금액이_일치한다() throws Exception {
        // expense-summary 는 원래부터 "오늘까지"만 집계한다. expense-report(진행 중인 달)도
        // 같은 기준을 써야 하므로, 시드에 미래 날짜 거래가 섞여 있어도 두 값이 항상 같아야 한다.
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");

        String summaryResponse = mvc.perform(get(SUMMARY).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long summaryTotal = objectMapper.readTree(summaryResponse).path("data").path("currentMonthTotal").asLong();

        String reportResponse = mvc.perform(get(REPORT).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode reportData = objectMapper.readTree(reportResponse).path("data");
        long reportTotal = reportData.path("summary").path("totalExpense").asLong();
        long lastTrendPoint = reportData.path("monthlyTrend").path("months").get(2).path("totalExpense").asLong();

        assertThat(reportTotal).isEqualTo(summaryTotal);
        assertThat(lastTrendPoint).isEqualTo(summaryTotal);
    }

    // --- 지출 코칭: 조회 중인 달이 아니라 그 전달(prevMonth) 지출을, prevMonth 이전 3개월
    //     평균(prevMonth 자신은 제외)과 비교해 판정한다 ---

    @Test
    void 코칭_증감률이_20_초과면_적자_코칭을_내려준다() throws Exception {
        // demo1: CLOSED_MONTH(2026-08)를 조회하면 그 전달인 2026-07 기준으로 판정한다.
        // 평균(4~6월: 827,800 / 862,900 / 877,300)은 856,000. 7월 지출 1,032,400 -> 증감률 약 +20.6%.
        // 6월 대비 7월 증가액: FOOD +100,800, LEISURE_SHOPPING +34,600 (가장 큰 두 카테고리).
        mvc.perform(get(REPORT).param("month", CLOSED_MONTH)
                        .header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coaching.tier").value("DEFICIT"))
                .andExpect(jsonPath("$.data.coaching.changeRate").value(21))
                .andExpect(jsonPath("$.data.coaching.savedAmount").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.coaching.excessAmount").value(176400))
                .andExpect(jsonPath("$.data.coaching.surgeCategories.length()").value(2))
                .andExpect(jsonPath("$.data.coaching.surgeCategories[0].category").value("FOOD"))
                .andExpect(jsonPath("$.data.coaching.surgeCategories[0].increaseAmount").value(100800))
                .andExpect(jsonPath("$.data.coaching.surgeCategories[1].category").value("LEISURE_SHOPPING"))
                .andExpect(jsonPath("$.data.coaching.surgeCategories[1].increaseAmount").value(34600))
                .andExpect(jsonPath("$.data.coaching.reductionTargetAmount").value(67700));
    }

    @Test
    void 코칭_증감률이_10에서_20_사이면_주의_코칭을_내려준다() throws Exception {
        // demo2: CLOSED_MONTH(2026-08)를 조회하면 그 전달인 2026-07 기준으로 판정한다.
        // 평균(4~6월: 728,400 / 733,500 / 746,700)은 736,200. 7월 지출 823,600 -> 증감률 약 +11.9%.
        // 6월 대비 7월 증가액이 가장 큰 카테고리는 FOOD(+38,900) 하나만 잡는다.
        mvc.perform(get(REPORT).param("month", CLOSED_MONTH)
                        .header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coaching.tier").value("CAUTION"))
                .andExpect(jsonPath("$.data.coaching.changeRate").value(12))
                .andExpect(jsonPath("$.data.coaching.surgeCategories.length()").value(1))
                .andExpect(jsonPath("$.data.coaching.surgeCategories[0].category").value("FOOD"))
                .andExpect(jsonPath("$.data.coaching.surgeCategories[0].increaseAmount").value(38900))
                .andExpect(jsonPath("$.data.coaching.reductionTargetAmount").value(19450));
    }

    @Test
    void 코칭_증감률이_10_이내면_코칭이_없다() throws Exception {
        // demo2: 2026-07을 조회하면 그 전달인 2026-06 기준으로 판정한다.
        // 평균(3~5월: 767,800 / 728,400 / 733,500)은 743,233.33. 6월 지출 746,700 -> 증감률 약 +0.5%(평소 수준) -> 코칭 없음
        mvc.perform(get(REPORT).param("month", "2026-07")
                        .header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coaching").value(Matchers.nullValue()));
    }

    @Test
    void 코칭_평균을_낼_이전_달_데이터가_없으면_코칭이_없다() throws Exception {
        // 시드 데이터는 2026-03부터 시작해서, 그 전달인 2026-02는 물론 평균을 낼 2025년 데이터도 없다 -> 판정 불가
        mvc.perform(get(REPORT).param("month", "2026-03")
                        .header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coaching").value(Matchers.nullValue()));
    }

    @Test
    void 코칭_증감률이_마이너스_10_이하면_저축여력_코칭을_내려준다() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        List<FinancialTransaction> inserted = List.of(
                newTxn(1L, LocalDate.of(2028, 7, 15), "EXPENSE", 300000, "장보기", "FOOD"),
                newTxn(1L, LocalDate.of(2028, 8, 15), "EXPENSE", 300000, "장보기", "FOOD"),
                newTxn(1L, LocalDate.of(2028, 9, 15), "EXPENSE", 300000, "장보기", "FOOD"),
                newTxn(1L, LocalDate.of(2028, 10, 15), "EXPENSE", 200000, "장보기", "FOOD")
        );
        transactionRepository.saveAll(inserted);
        try {
            // 평균(7~9월: 30만원씩) 300,000, 전달(10월) 지출 200,000 -> 증감률 -33% -> 저축여력, 적게 쓴 금액 100,000
            mvc.perform(get(REPORT).param("month", SURPLUS_MONTH).header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.coaching.tier").value("SURPLUS"))
                    .andExpect(jsonPath("$.data.coaching.changeRate").value(-33))
                    .andExpect(jsonPath("$.data.coaching.savedAmount").value(100000))
                    .andExpect(jsonPath("$.data.coaching.excessAmount").value(Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.coaching.surgeCategories").isEmpty())
                    .andExpect(jsonPath("$.data.coaching.reductionTargetAmount").value(Matchers.nullValue()));
        } finally {
            transactionRepository.deleteAllById(inserted.stream().map(FinancialTransaction::getId).toList());
        }
    }

    private FinancialTransaction newTxn(long memberId, LocalDate date, String type, long amount, String merchant, String category) {
        FinancialTransaction t = new FinancialTransaction();
        t.setMemberId(memberId);
        t.setAccountId(1L);
        t.setTxnDate(date);
        t.setTxnType(type);
        t.setAmount(amount);
        t.setMerchantName(merchant);
        t.setCategory(category);
        return t;
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

    @Test
    void 예산이_설정된_달의_리포트에는_예산값이_채워진다() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        mvc.perform(delete(BUDGET).param("month", BUDGET_LINKED_MONTH).header("Authorization", auth));

        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":500000,
                                 "categories":[{"category":"FOOD","amount":200000}]}
                                """.formatted(BUDGET_LINKED_MONTH)))
                .andExpect(status().isOk());

        mvc.perform(get(REPORT).param("month", BUDGET_LINKED_MONTH).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyBudget").value(500000))
                .andExpect(jsonPath("$.data.categories[1].category").value("FOOD"))
                .andExpect(jsonPath("$.data.categories[1].budget").value(200000))
                .andExpect(jsonPath("$.data.categories[0].category").value("HOUSING_UTILITY"))
                .andExpect(jsonPath("$.data.categories[0].budget").value(Matchers.nullValue()));

        mvc.perform(delete(BUDGET).param("month", BUDGET_LINKED_MONTH).header("Authorization", auth));
    }
}
