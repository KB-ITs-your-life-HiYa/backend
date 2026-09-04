package com.fledge.member;

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
class AuthApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String ME = "/api/v1/members/me";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private String body(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    private String loginAndGetToken(String email) throws Exception {
        String response = mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "demo1234")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void 로그인하면_토큰과_회원정보를_받는다() throws Exception {
        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("demo2@fledge.dev", "demo1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.member.email").value("demo2@fledge.dev"))
                .andExpect(jsonPath("$.data.member.tier").value("SELF_RELIANCE"));
    }

    @Test
    void 비밀번호가_틀리면_401() throws Exception {
        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("demo2@fledge.dev", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
    }

    @Test
    void 없는_이메일도_같은_응답이다() throws Exception {
        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("nobody@fledge.dev", "demo1234")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
    }

    @Test
    void 형식이_틀리면_400() throws Exception {
        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("notanemail", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void 토큰_없이_내정보를_조회하면_401() throws Exception {
        mvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 위조된_토큰이면_401() throws Exception {
        String token = loginAndGetToken("demo2@fledge.dev");
        String forged = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        mvc.perform(get(ME).header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰의_주인만_조회된다() throws Exception {
        mvc.perform(get(ME).header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(jsonPath("$.data.memberId").value(1));

        mvc.perform(get(ME).header("Authorization", "Bearer " + loginAndGetToken("demo2@fledge.dev")))
                .andExpect(jsonPath("$.data.memberId").value(2));
    }

    @Test
    void 보호종료일과_거주지역_코드가_내려온다() throws Exception {
        mvc.perform(get(ME).header("Authorization", "Bearer " + loginAndGetToken("demo1@fledge.dev")))
                .andExpect(jsonPath("$.data.protectionEndDate").isNotEmpty())
                .andExpect(jsonPath("$.data.homeRegionCode").value("41"));
    }
}