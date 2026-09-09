package com.fledge.counselor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CounselorYouthApiTest {
    private static final String YOUTHS = "/api/v1/members/me/counselor/youths";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void 상담사는_현재_배정된_두_청년을_조회한다() throws Exception {
        mvc.perform(get(YOUTHS)
                        .header("Authorization", "Bearer " + counselorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("김도윤"))
                .andExpect(jsonPath("$.data[0].phone").value("010-2841-7723"))
                .andExpect(jsonPath("$.data[0].regionName").value("경기도 수원시 팔달구"))
                .andExpect(jsonPath("$.data[0].protectionStatus").value("ENDED"))
                .andExpect(jsonPath("$.data[0].daysUntilSupportEnd").isNumber())
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("이서윤"))
                .andExpect(jsonPath("$.data[1].regionName").value("인천광역시 미추홀구"));
    }

    @Test
    void 배정이_종료된_청년은_목록에서_제외한다() throws Exception {
        jdbc.update("""
                UPDATE counselor_youth_assignment
                SET unassigned_at = now()
                WHERE youth_member_id = 2
                  AND unassigned_at IS NULL
                """);

        mvc.perform(get(YOUTHS)
                        .header("Authorization", "Bearer " + counselorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void 청년회원은_담당_청년_목록을_조회할_수_없다() throws Exception {
        mvc.perform(get(YOUTHS)
                        .header("Authorization", "Bearer " + login("demo1@fledge.dev", "demo1234")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private String counselorToken() throws Exception {
        return login("counselor@fledge.local", "counselor1234");
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }
}
