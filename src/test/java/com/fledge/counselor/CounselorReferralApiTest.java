package com.fledge.counselor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class CounselorReferralApiTest {
    private static final String REFERRALS = "/api/v1/members/me/counselor/referrals";
    private static final long TEST_ID = 990001L;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM money_schedule WHERE id = ?", TEST_ID);
        jdbc.update("""
                INSERT INTO money_schedule (
                    id, member_id, direction, type, name, expected_amount, expected_day,
                    match_keyword, is_active, created_at, updated_at)
                VALUES (?, 2, 'OUT', 'SAVINGS', 'KB청년미래적금', 200000, 25,
                        'KB청년미래적금', true, now(), now())
                """, TEST_ID);
        jdbc.update("""
                INSERT INTO money_cycle (
                    id, schedule_id, member_id, cycle_month, expected_date,
                    expected_amount, status, updated_at)
                VALUES (?, ?, 2, date_trunc('month', CURRENT_DATE)::date,
                        CURRENT_DATE - 1, 200000, 'MISSED', now())
                """, TEST_ID, TEST_ID);
        jdbc.update("""
                INSERT INTO care_signal (
                    id, member_id, money_cycle_id, signal_type, status,
                    response_result, detected_at, created_at, updated_at)
                VALUES (?, 2, ?, 'MISSED_SAVING', 'OPEN', 'NEEDS_CARE',
                        now(), now(), now())
                """, TEST_ID, TEST_ID);
        jdbc.update("""
                INSERT INTO care_response (
                    id, care_signal_id, input_type, input_text, ai_reply,
                    created_at, ai_status, request_id, request_payload)
                VALUES (?, ?, 'FREE_TEXT', '이번 달에는 생활비가 부족해서 적금을 내기 어려워요',
                        '현재 상황을 담당자에게 전달할 수 있어요', now(),
                        'READY', 'counselor-api-test', 'test')
                """, TEST_ID, TEST_ID);
        jdbc.update("""
                INSERT INTO referral_request (
                    id, member_id, care_signal_id, counselor_id, status,
                    reason, risk_score_at_request, requested_at)
                SELECT ?, 2, ?, counselor.id, 'REQUESTED', 'HIGH_RISK', 65,
                       now() + INTERVAL '1 day'
                FROM counselor
                WHERE member_id = 3
                """, TEST_ID, TEST_ID);
    }

    @Test
    void 상담사는_자신에게_전달된_현재상황을_조회한다() throws Exception {
        mvc.perform(get(REFERRALS)
                        .header("Authorization", "Bearer " + login("counselor@fledge.local", "counselor1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(TEST_ID))
                .andExpect(jsonPath("$.data[0].youthId").value(2))
                .andExpect(jsonPath("$.data[0].youthName").value("이서윤"))
                .andExpect(jsonPath("$.data[0].status").value("REQUESTED"))
                .andExpect(jsonPath("$.data[0].riskLevel").value("HUMAN_CARE"))
                .andExpect(jsonPath("$.data[0].signalType").value("MISSED_SAVING"))
                .andExpect(jsonPath("$.data[0].situation").value("KB청년미래적금 적금 납입이 확인되지 않았습니다"))
                .andExpect(jsonPath("$.data[0].latestUserMessage")
                        .value("이번 달에는 생활비가 부족해서 적금을 내기 어려워요"));
    }

    @Test
    void 청년회원은_상담사_요청목록을_조회할_수_없다() throws Exception {
        mvc.perform(get(REFERRALS)
                        .header("Authorization", "Bearer " + login("demo2@fledge.dev", "demo1234")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
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
