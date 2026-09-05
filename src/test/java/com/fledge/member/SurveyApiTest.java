package com.fledge.member;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.member.repository.MemberSurveyRepository;
import com.fledge.member.repository.MemberSurveyTagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트가 끝나면 롤백돼 demo 계정 설문 데이터를 건드리지 않는다
class SurveyApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String SURVEY = "/api/v1/members/me/survey";
    private static final Long DEMO2_ID = 2L;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberSurveyRepository memberSurveyRepository;
    @Autowired MemberSurveyTagRepository memberSurveyTagRepository;

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
    void 저장된_설문이_없으면_null_이_내려온다() throws Exception {
        memberSurveyTagRepository.deleteByMemberId(DEMO2_ID);
        memberSurveyRepository.deleteById(DEMO2_ID);

        String token = loginAndGetToken("demo2@fledge.dev");
        String response = mvc.perform(get(SURVEY).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(response).path("data").isNull()).isTrue();
    }

    @Test
    void 설문을_저장하면_조회에서_그대로_돌아온다() throws Exception {
        String token = loginAndGetToken("demo2@fledge.dev");

        mvc.perform(post(SURVEY)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"householdSize":3,"incomePctBracket":60,"isBenefitRecipient":true,
                                 "employmentStatus":"SEEKING","housingType":"MONTHLY_RENT","tags":["MULTI_CHILD"]}"""))
                .andExpect(status().isOk());

        String response = mvc.perform(get(SURVEY).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.path("householdSize").asInt()).isEqualTo(3);
        assertThat(data.path("incomePctBracket").asInt()).isEqualTo(60);
        assertThat(data.path("isBenefitRecipient").asBoolean()).isTrue();
        assertThat(data.path("employmentStatus").asText()).isEqualTo("SEEKING");
        assertThat(data.path("housingType").asText()).isEqualTo("MONTHLY_RENT");
        assertThat(data.path("tags").get(0).asText()).isEqualTo("MULTI_CHILD");
    }
}
