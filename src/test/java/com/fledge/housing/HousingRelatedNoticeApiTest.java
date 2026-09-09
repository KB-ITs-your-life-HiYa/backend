package com.fledge.housing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/db/seed/R__seed_02_housing.sql")
class HousingRelatedNoticeApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String RELATED = "/api/v1/housing/notices/related-to-subsidy/{subsidyId}";

    // R__seed_07_subsidy.sql 의 "기존주택 전세임대주택 지원사업" — 이름에 "전세임대"가 들어있다
    private static final long JEONSE_SUBSIDY_ID = 4L;
    // "자립준비청년(보호종료아동) 자립수당 지급" — 공급유형 이름이 안 들어있어 관련 공고가 없어야 한다
    private static final long ALLOWANCE_SUBSIDY_ID = 19L;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private String loginAndGetToken() throws Exception {
        String response = mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo1@fledge.dev","password":"demo1234"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void 이름에_공급유형이_들어간_지원금은_관련_공고를_찾는다() throws Exception {
        mvc.perform(get(RELATED, JEONSE_SUBSIDY_ID).header("Authorization", "Bearer " + loginAndGetToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title")
                        .value("2026년 자립준비청년(청년 유형) 전세임대 입주자 수시모집"));
    }

    @Test
    void 공급유형이_안_들어간_지원금은_빈_배열이다() throws Exception {
        mvc.perform(get(RELATED, ALLOWANCE_SUBSIDY_ID).header("Authorization", "Bearer " + loginAndGetToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 없는_지원금이면_404() throws Exception {
        mvc.perform(get(RELATED, 999_999_999L).header("Authorization", "Bearer " + loginAndGetToken()))
                .andExpect(status().isNotFound());
    }
}
