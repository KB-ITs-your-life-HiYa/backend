package com.fledge.housing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.housing.collect.HousingCollector;
import com.fledge.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HousingAdminApiTest {
    private static final String COLLECT = "/api/v1/housing/admin/collect";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenProvider tokens;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean HousingCollector collector;

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    void 비로그인은_401이고_수집하지_않는다() throws Exception {
        mvc.perform(post(COLLECT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verifyNoInteractions(collector);
    }

    @Test
    void 잘못된_토큰은_401이고_수집하지_않는다() throws Exception {
        mvc.perform(post(COLLECT).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(collector);
    }

    @Test
    void 일반_회원은_403이고_수집하지_않는다() throws Exception {
        String token = login("demo1@fledge.dev", "demo1234");
        mvc.perform(post(COLLECT).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(collector);
    }

    @Test
    void 상담사는_수집을_실행하고_결과를_받는다() throws Exception {
        when(collector.collect()).thenReturn(new HousingCollector.CollectResult(2, 3, 0, 0));
        String token = login("counselor@fledge.local", "counselor1234");
        mvc.perform(post(COLLECT).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notices").value(2))
                .andExpect(jsonPath("$.data.units").value(3));
        verify(collector).collect();
    }

    @Test
    void 존재하지_않는_회원의_토큰은_수집할_수_없다() throws Exception {
        mvc.perform(post(COLLECT).header("Authorization", "Bearer " + tokens.createToken(Long.MAX_VALUE)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(collector);
    }

    @Test
    @Transactional
    void 토큰이_유효해도_상담사_역할이_해제되면_403이다() throws Exception {
        String token = tokens.createToken(3L);
        jdbc.update("update member set role = 'YOUTH' where id = 3");
        mvc.perform(post(COLLECT).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(collector);
    }
}
