package com.fledge.housing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HousingChecklistApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String BASE = "/api/v1/members/me/housing/checklists";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private String token() throws Exception {
        String response = mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo2@fledge.dev","password":"demo1234"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void 템플릿으로_생성하고_목록_진행률_항목_CRUD를_한다() throws Exception {
        String auth = "Bearer " + token();

        // 기존 데이터 정리 (재실행 가능한 테스트)
        MvcResult listed = mvc.perform(get(BASE).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode node : objectMapper.readTree(listed.getResponse().getContentAsString()).path("data")) {
            mvc.perform(delete(BASE + "/" + node.path("id").asLong()).header("Authorization", auth))
                    .andExpect(status().isOk());
        }

        MvcResult created = mvc.perform(post(BASE)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateType":"HOUSE_HUNTING"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateType").value("HOUSE_HUNTING"))
                .andExpect(jsonPath("$.data.title").value("좋은 집 찾기"))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.doneCount").value(0))
                .andExpect(jsonPath("$.data.progress").value(0.0))
                .andExpect(jsonPath("$.data.items", hasSize(5)))
                .andExpect(jsonPath("$.data.items[0].content").value("내가 낼 수 있는 돈부터 계산하기"))
                .andReturn();

        long checklistId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long itemId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        // 같은 종류 중복 생성 → 409
        mvc.perform(post(BASE)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateType":"HOUSE_HUNTING"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("HOUSING_CHECKLIST_ALREADY_EXISTS"));

        // 완료 토글
        mvc.perform(patch(BASE + "/" + checklistId + "/items/" + itemId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"done":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.done").value(true));

        mvc.perform(get(BASE).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].doneCount").value(1))
                .andExpect(jsonPath("$.data[0].progress").value(0.2));

        // 항목 추가
        MvcResult added = mvc.perform(post(BASE + "/" + checklistId + "/items")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"부동산 중개수수료 확인","memo":"협의 가능 여부"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.content").value("부동산 중개수수료 확인"))
                .andReturn();
        long addedId = objectMapper.readTree(added.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 항목 삭제
        mvc.perform(delete(BASE + "/{id}/items/{itemId}", checklistId, addedId)
                        .header("Authorization", auth))
                .andExpect(status().isOk());

        mvc.perform(get(BASE).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalCount").value(5));

        // 체크리스트 삭제
        mvc.perform(delete(BASE + "/" + checklistId).header("Authorization", auth))
                .andExpect(status().isOk());

        mvc.perform(get(BASE).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void 입주_이사_템플릿도_생성된다() throws Exception {
        String auth = "Bearer " + token();

        MvcResult listed = mvc.perform(get(BASE).header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode node : objectMapper.readTree(listed.getResponse().getContentAsString()).path("data")) {
            mvc.perform(delete(BASE + "/" + node.path("id").asLong()).header("Authorization", auth))
                    .andExpect(status().isOk());
        }

        mvc.perform(post(BASE)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateType":"MOVE_IN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("입주 준비"))
                .andExpect(jsonPath("$.data.items", hasSize(5)));

        mvc.perform(post(BASE)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateType":"MOVING"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("이사 준비"))
                .andExpect(jsonPath("$.data.items", hasSize(5)));
    }

    @Test
    void 없는_체크리스트는_404() throws Exception {
        String auth = "Bearer " + token();
        mvc.perform(delete(BASE + "/999999999").header("Authorization", auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HOUSING_CHECKLIST_NOT_FOUND"));
    }
}
