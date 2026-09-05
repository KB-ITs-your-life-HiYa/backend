package com.fledge.housing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.repository.HousingNoticeRepository;
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
class HousingNoticeDetailApiTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String DETAIL = "/api/v1/housing/notices/{id}";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired HousingNoticeRepository noticeRepository;

    private String loginAndGetToken() throws Exception {
        String response = mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo2@fledge.dev","password":"demo1234"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private Long seedNoticeId(String pblancId) {
        return noticeRepository.findByPblancId(pblancId)
                .map(HousingNotice::getId)
                .orElseThrow(() -> new IllegalStateException("시드 공고가 없습니다: " + pblancId));
    }

    @Test
    void 공고_상세와_단지_목록을_조회한다() throws Exception {
        Long id = seedNoticeId("SEED-SR-002");

        mvc.perform(get(DETAIL, id).header("Authorization", "Bearer " + loginAndGetToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.title")
                        .value("2026년 보호종료아동 우선공급 매입임대주택 예비입주자 모집공고"))
                .andExpect(jsonPath("$.data.institution").value("LH"))
                .andExpect(jsonPath("$.data.houseType").value("아파트"))
                .andExpect(jsonPath("$.data.supplyType").value("매입임대"))
                .andExpect(jsonPath("$.data.targetType").value("SELF_RELIANCE"))
                .andExpect(jsonPath("$.data.superseded").value(false))
                .andExpect(jsonPath("$.data.contact").value("LH 콜센터 : 1600-1004 (평일 : 09:00 ~ 18:00)"))
                .andExpect(jsonPath("$.data.applyUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.myhomeUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.units.length()").value(2))
                .andExpect(jsonPath("$.data.units[0].complexName").value("인천주안 A-2BL"))
                .andExpect(jsonPath("$.data.units[0].region").value("인천광역시"))
                .andExpect(jsonPath("$.data.units[0].district").value("미추홀구"))
                .andExpect(jsonPath("$.data.units[0].deposit").value(3200000))
                .andExpect(jsonPath("$.data.units[0].monthlyRent").value(152000))
                .andExpect(jsonPath("$.data.units[1].complexName").value("수원권선 B-1BL"))
                .andExpect(jsonPath("$.data.units[1].region").value("경기도"));
    }

    @Test
    void 없는_공고면_404() throws Exception {
        mvc.perform(get(DETAIL, 999_999_999L)
                        .header("Authorization", "Bearer " + loginAndGetToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HOUSING_NOTICE_NOT_FOUND"));
    }

    @Test
    void 토큰_없이_조회하면_401() throws Exception {
        Long id = seedNoticeId("SEED-SR-001");

        mvc.perform(get(DETAIL, id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
