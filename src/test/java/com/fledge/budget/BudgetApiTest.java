package com.fledge.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BudgetApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String BUDGET = "/api/v1/members/me/budget";

    // demo1 시드 데이터 기준 이미 끝난 달(2026-08)의 고정 지출. "오늘"과 무관하게 항상 같다
    private static final String UNSET_MONTH = "2026-09"; // 예산을 만들지 않는 조회 전용 달 (전달=2026-08 지출 검증용)

    // 저장/삭제/검증 테스트 전용 달. 리포트·요약 테스트와 겹치지 않도록 별도로 둔다
    private static final String FIXTURE_MONTH = "2027-03";
    private static final String FIXTURE_MONTH_2 = "2027-04";
    private static final String FIXTURE_MONTH_3 = "2027-05";
    private static final String NEVER_SET_MONTH = "2027-06";

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

    private void deleteIfExists(String auth, String month) throws Exception {
        mvc.perform(delete(BUDGET).param("month", month).header("Authorization", auth));
    }

    @Test
    void 예산_미설정_상태를_조회하면_지난달_사용액만_내려온다() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");

        mvc.perform(get(BUDGET).param("month", UNSET_MONTH).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(UNSET_MONTH))
                .andExpect(jsonPath("$.data.totalAmount").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.categories.length()").value(6))
                .andExpect(jsonPath("$.data.categories[0].category").value("HOUSING_UTILITY"))
                .andExpect(jsonPath("$.data.categories[0].amount").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.categories[0].lastMonthAmount").value(236700))
                .andExpect(jsonPath("$.data.categories[1].category").value("FOOD"))
                .andExpect(jsonPath("$.data.categories[1].lastMonthAmount").value(281300))
                .andExpect(jsonPath("$.data.categories[2].category").value("TRANSPORT"))
                .andExpect(jsonPath("$.data.categories[2].lastMonthAmount").value(80000))
                .andExpect(jsonPath("$.data.categories[3].category").value("LIVING_MEDICAL"))
                .andExpect(jsonPath("$.data.categories[3].lastMonthAmount").value(64900))
                .andExpect(jsonPath("$.data.categories[4].category").value("LEISURE_SHOPPING"))
                .andExpect(jsonPath("$.data.categories[4].lastMonthAmount").value(128700))
                .andExpect(jsonPath("$.data.categories[5].category").value("SAVINGS"))
                .andExpect(jsonPath("$.data.categories[5].lastMonthAmount").value(50000));
    }

    @Test
    void 예산을_저장하고_조회한다() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        deleteIfExists(auth, FIXTURE_MONTH);

        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "month": "%s",
                                  "totalAmount": 800000,
                                  "categories": [
                                    {"category":"HOUSING_UTILITY","amount":300000},
                                    {"category":"FOOD","amount":200000}
                                  ]
                                }""".formatted(FIXTURE_MONTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(800000))
                .andExpect(jsonPath("$.data.categories[0].amount").value(300000))
                .andExpect(jsonPath("$.data.categories[1].amount").value(200000))
                .andExpect(jsonPath("$.data.categories[2].amount").value(Matchers.nullValue()));

        mvc.perform(get(BUDGET).param("month", FIXTURE_MONTH).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(800000))
                .andExpect(jsonPath("$.data.categories[0].amount").value(300000))
                .andExpect(jsonPath("$.data.categories[1].amount").value(200000));

        deleteIfExists(auth, FIXTURE_MONTH);
    }

    @Test
    void 같은_달에_다시_저장하면_통째로_갈아끼운다() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        deleteIfExists(auth, FIXTURE_MONTH_3);

        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":500000,
                                 "categories":[{"category":"FOOD","amount":100000},{"category":"TRANSPORT","amount":50000}]}
                                """.formatted(FIXTURE_MONTH_3)))
                .andExpect(status().isOk());

        // FOOD 는 0 으로(=미설정), TRANSPORT 는 아예 배열에서 제외 -> 둘 다 사라져야 한다
        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":500000,
                                 "categories":[{"category":"FOOD","amount":0},{"category":"LIVING_MEDICAL","amount":70000}]}
                                """.formatted(FIXTURE_MONTH_3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[1].category").value("FOOD"))
                .andExpect(jsonPath("$.data.categories[1].amount").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.categories[2].category").value("TRANSPORT"))
                .andExpect(jsonPath("$.data.categories[2].amount").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.categories[3].category").value("LIVING_MEDICAL"))
                .andExpect(jsonPath("$.data.categories[3].amount").value(70000));

        // 이미 존재하는 카테고리(LIVING_MEDICAL)를 다른 금액으로 다시 저장 — 같은 카테고리가
        // 두 번의 저장에 걸쳐 남아 있을 때 유니크 제약에 걸리지 않는지 확인한다
        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":500000,
                                 "categories":[{"category":"LIVING_MEDICAL","amount":90000}]}
                                """.formatted(FIXTURE_MONTH_3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[3].category").value("LIVING_MEDICAL"))
                .andExpect(jsonPath("$.data.categories[3].amount").value(90000));

        deleteIfExists(auth, FIXTURE_MONTH_3);
    }

    @Test
    void 카테고리_합계가_총예산을_초과하면_400() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        deleteIfExists(auth, FIXTURE_MONTH);

        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":100000,
                                 "categories":[{"category":"FOOD","amount":60000},{"category":"TRANSPORT","amount":60000}]}
                                """.formatted(FIXTURE_MONTH)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUDGET_CATEGORY_SUM_EXCEEDS_TOTAL"));
    }

    @Test
    void SAVINGS는_카테고리_합계_검증에서_제외된다() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        deleteIfExists(auth, FIXTURE_MONTH);

        // FOOD(300000) 만 총예산(500000) 이내 -> SAVINGS(400000) 를 더하면 초과지만 통과해야 한다
        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":500000,
                                 "categories":[{"category":"FOOD","amount":300000},{"category":"SAVINGS","amount":400000}]}
                                """.formatted(FIXTURE_MONTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[5].category").value("SAVINGS"))
                .andExpect(jsonPath("$.data.categories[5].amount").value(400000));

        deleteIfExists(auth, FIXTURE_MONTH);
    }

    @Test
    void 총예산_없이_카테고리만_오면_400() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");

        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","categories":[{"category":"FOOD","amount":100000}]}
                                """.formatted(FIXTURE_MONTH)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 예산을_삭제한다() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        deleteIfExists(auth, FIXTURE_MONTH);

        mvc.perform(put(BUDGET)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":300000,"categories":[]}
                                """.formatted(FIXTURE_MONTH)))
                .andExpect(status().isOk());

        mvc.perform(delete(BUDGET).param("month", FIXTURE_MONTH).header("Authorization", auth))
                .andExpect(status().isOk());

        mvc.perform(get(BUDGET).param("month", FIXTURE_MONTH).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(Matchers.nullValue()));
    }

    @Test
    void 없는_달의_예산을_삭제하면_404() throws Exception {
        String auth = "Bearer " + loginAndGetToken("demo1@fledge.dev");

        mvc.perform(delete(BUDGET).param("month", NEVER_SET_MONTH).header("Authorization", auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BUDGET_NOT_FOUND"));
    }

    @Test
    void 토큰_없이_요청하면_401() throws Exception {
        mvc.perform(get(BUDGET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 다른_회원의_예산은_섞이지_않는다() throws Exception {
        String auth1 = "Bearer " + loginAndGetToken("demo1@fledge.dev");
        String auth2 = "Bearer " + loginAndGetToken("demo2@fledge.dev");
        deleteIfExists(auth1, FIXTURE_MONTH_2);
        deleteIfExists(auth2, FIXTURE_MONTH_2);

        mvc.perform(put(BUDGET)
                        .header("Authorization", auth1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","totalAmount":900000,"categories":[]}
                                """.formatted(FIXTURE_MONTH_2)))
                .andExpect(status().isOk());

        // demo2 는 같은 달에 예산을 저장한 적이 없으므로 미설정으로 보여야 한다
        mvc.perform(get(BUDGET).param("month", FIXTURE_MONTH_2).header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(Matchers.nullValue()));

        // demo2 가 그 달을 삭제하려 해도 자기 예산이 없으므로 404
        mvc.perform(delete(BUDGET).param("month", FIXTURE_MONTH_2).header("Authorization", auth2))
                .andExpect(status().isNotFound());

        // demo1 의 예산은 그대로 남아 있어야 한다
        mvc.perform(get(BUDGET).param("month", FIXTURE_MONTH_2).header("Authorization", auth1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(900000));

        deleteIfExists(auth1, FIXTURE_MONTH_2);
    }
}