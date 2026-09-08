package com.fledge.member;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트가 끝나면 롤백돼 demo 계정 데이터를 건드리지 않는다
class MemberSubsidyApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String SUBSIDIES = "/api/v1/members/me/subsidies";
    private static final String SEARCH = "/api/v1/subsidies";
    private static final String MATCHES = "/api/v1/members/me/benefit/matches";

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

    private Long firstSearchResultId(String token) throws Exception {
        String response = mvc.perform(get(SEARCH).param("q", "자립수당").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").get(0).path("subsidyId").asLong();
    }

    @Test
    void 지원금_이름으로_검색한다() throws Exception {
        String token = loginAndGetToken("demo1@fledge.dev");

        String response = mvc.perform(get(SEARCH).param("q", "자립수당").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(response).path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
        assertThat(data.get(0).path("name").asText()).contains("자립수당");
    }

    @Test
    void 받고_있는_지원금을_추가하고_삭제한다() throws Exception {
        String token = loginAndGetToken("demo1@fledge.dev");
        Long subsidyId = firstSearchResultId(token);

        mvc.perform(post(SUBSIDIES)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subsidyId\":" + subsidyId + "}"))
                .andExpect(status().isOk());

        String listResponse = mvc.perform(get(SUBSIDIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode list = objectMapper.readTree(listResponse).path("data");
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).path("subsidyId").asLong()).isEqualTo(subsidyId);

        mvc.perform(delete(SUBSIDIES + "/" + subsidyId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String afterDelete = mvc.perform(get(SUBSIDIES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(afterDelete).path("data").size()).isEqualTo(0);
    }

    @Test
    void 받고_있는_지원금은_매칭_목록에서_빠진다() throws Exception {
        String token = loginAndGetToken("demo1@fledge.dev");
        Long subsidyId = firstSearchResultId(token);

        mvc.perform(post(SUBSIDIES)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subsidyId\":" + subsidyId + "}"))
                .andExpect(status().isOk());

        String matchesResponse = mvc.perform(get(MATCHES).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode categories = objectMapper.readTree(matchesResponse).path("data");
        for (JsonNode category : categories) {
            for (JsonNode item : category.path("items")) {
                assertThat(item.path("subsidyId").asLong()).isNotEqualTo(subsidyId);
            }
        }
    }
}
