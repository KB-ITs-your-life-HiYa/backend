package com.fledge.housing;

import com.fledge.housing.repository.HousingEligibilityProfileRepository;
import com.fledge.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HousingEligibilityProfileApiTest {
    private static final String URL = "/api/v1/members/me/housing-eligibility";

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider tokens;
    @Autowired HousingEligibilityProfileRepository profiles;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void prepare() {
        // 테스트 종료 시 롤백하므로 기존 회원의 입력 정보는 보존된다.
        profiles.deleteById(1L);
        profiles.deleteById(2L);
        profiles.flush();
    }

    private String auth(long memberId) {
        return "Bearer " + tokens.createToken(memberId);
    }

    @Test
    void 미입력은_false가_아닌_null로_조회한다() throws Exception {
        mvc.perform(get(URL).header("Authorization", auth(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void 최초_저장_후_재조회하고_같은_행을_수정한다() throws Exception {
        mvc.perform(put(URL).header("Authorization", auth(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":true,\"isMarried\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHomeless").value(true))
                .andExpect(jsonPath("$.data.isMarried").value(false))
                .andExpect(jsonPath("$.data.youthPurchasePriorityBasis").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
        profiles.flush();
        entityManager.clear();
        mvc.perform(get(URL).header("Authorization", auth(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHomeless").value(true))
                .andExpect(jsonPath("$.data.isMarried").value(false));

        mvc.perform(put(URL).header("Authorization", auth(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":false,\"isMarried\":true}"))
                .andExpect(status().isOk());
        profiles.flush();
        entityManager.clear();
        var stored = profiles.findById(1L).orElseThrow();
        assertThat(stored.isHomeless()).isFalse();
        assertThat(stored.isMarried()).isTrue();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }

    @Test
    void 청년_매입임대_1순위_근거를_저장하고_구버전_요청에서도_보존한다() throws Exception {
        mvc.perform(put(URL).header("Authorization", auth(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":true,\"isMarried\":false,\"youthPurchasePriorityBasis\":\"NEAR_POVERTY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.youthPurchasePriorityBasis").value("NEAR_POVERTY"));

        mvc.perform(put(URL).header("Authorization", auth(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":false,\"isMarried\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.youthPurchasePriorityBasis").value("NEAR_POVERTY"));
    }

    @Test
    void 다른_회원의_정보를_조회하거나_덮어쓰지_않는다() throws Exception {
        mvc.perform(put(URL).header("Authorization", auth(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":true,\"isMarried\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get(URL).header("Authorization", auth(2)))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(put(URL).header("Authorization", auth(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":false,\"isMarried\":true}"))
                .andExpect(status().isOk());
        profiles.flush();
        entityManager.clear();
        mvc.perform(get(URL).header("Authorization", auth(1)))
                .andExpect(jsonPath("$.data.isHomeless").value(true))
                .andExpect(jsonPath("$.data.isMarried").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"isHomeless\":true}", "{\"isMarried\":false}",
            "{\"isHomeless\":null,\"isMarried\":false}",
            "{\"isHomeless\":true,\"isMarried\":null}"})
    void 누락된_답변은_저장하지_않는다(String body) throws Exception {
        mvc.perform(put(URL).header("Authorization", auth(1))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(profiles.existsById(1L)).isFalse();
    }

    @Test
    void 인증이_없으면_조회와_저장을_거절한다() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":true,\"isMarried\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 존재하지_않는_회원의_토큰을_거절한다() throws Exception {
        mvc.perform(get(URL).header("Authorization", auth(999999999L)))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(URL).header("Authorization", auth(999999999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":true,\"isMarried\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
