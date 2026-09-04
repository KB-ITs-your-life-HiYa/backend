package com.fledge.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String SUMMARY = "/api/v1/members/me/accounts/summary";
    private static final String ACCOUNTS = "/api/v1/members/me/accounts";

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
    void 자산_요약을_조회한다() throws Exception {
        mvc.perform(get(SUMMARY).header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depositTotal").value(6873400))
                .andExpect(jsonPath("$.data.savingsTotal").value(900000))
                .andExpect(jsonPath("$.data.netAsset").value(7773400));
    }

    @Test
    void 입출금_계좌_목록을_조회한다() throws Exception {
        mvc.perform(get(ACCOUNTS).param("type", "DEPOSIT")
                        .header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.totalBalance").value(6873400))
                .andExpect(jsonPath("$.data.accounts.length()").value(2))
                .andExpect(jsonPath("$.data.accounts[0].bankName").value("KB국민 주거래 통장"))
                .andExpect(jsonPath("$.data.accounts[0].balance").value(5487200));
    }

    @Test
    void 예적금_계좌_목록을_조회한다() throws Exception {
        mvc.perform(get(ACCOUNTS).param("type", "SAVINGS")
                        .header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.data.totalBalance").value(900000))
                .andExpect(jsonPath("$.data.accounts.length()").value(1))
                .andExpect(jsonPath("$.data.accounts[0].bankName").value("KB국민 자유적금"));
    }

    @Test
    void 잘못된_type이면_400() throws Exception {
        mvc.perform(get(ACCOUNTS).param("type", "NOPE")
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
    void 로그인한_회원의_계좌만_합산한다() throws Exception {
        mvc.perform(get(SUMMARY).header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depositTotal").value(3056400))
                .andExpect(jsonPath("$.data.savingsTotal").value(3580000))
                .andExpect(jsonPath("$.data.netAsset").value(6636400));
    }
}
