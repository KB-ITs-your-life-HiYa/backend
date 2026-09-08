package com.fledge.housing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.housing.repository.HousingEligibilityProfileRepository;
import com.fledge.housing.repository.HousingNoticeRepository;
import com.fledge.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/db/seed/R__seed_02_housing.sql")
class HousingEligibilityApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtTokenProvider tokens;
    @Autowired HousingNoticeRepository notices;
    @Autowired HousingEligibilityProfileRepository profiles;
    @Autowired JdbcTemplate jdbc;

    @Test
    void 정보_수정이_상세와_모든_캘린더_경로에_반영되고_다른회원은_격리된다() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        jdbc.update("UPDATE member SET protection_status='ENDED', protection_type='FACILITY', protection_end_date=? WHERE id=1",
                today.minusYears(2));
        profiles.deleteById(1L);
        profiles.deleteById(2L);
        profiles.flush();
        long id = notices.findByPblancId("SEED-SR-001").orElseThrow().getId();
        checkDetail(id, 1, "NEEDS_CHECK");
        save(true, false);
        checkDetail(id, 1, "MATCH");
        // ALL、명시 지역、회원 기본 지역(공고 부족 시 전국 전환) 모두 검사한다.
        for (String region : new String[]{"ALL", "28", ""}) {
            var request = get("/api/v1/housing/calendar").param("year", "" + today.getYear())
                    .param("month", "" + today.getMonthValue())
                    .header("Authorization", "Bearer " + tokens.createToken(1L));
            if (!region.isEmpty()) request.param("regionCode", region);
            String body = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            JsonNode data = mapper.readTree(body).path("data");
            boolean found = false;
            for (String collection : new String[]{"notices", "ongoingNotices"}) {
                for (JsonNode notice : data.path(collection)) {
                    if (notice.path("id").asLong() == id) {
                        found = true;
                        assertThat(notice.path("eligibility").path("status").asText()).isEqualTo("MATCH");
                    }
                }
            }
            assertThat(found).isTrue();
        }
        checkDetail(id, 2, "NEEDS_CHECK");
        save(false, false);
        checkDetail(id, 1, "NO_MATCH");
    }

    private void save(boolean homeless, boolean married) throws Exception {
        mvc.perform(put("/api/v1/members/me/housing-eligibility")
                        .header("Authorization", "Bearer " + tokens.createToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isHomeless\":%s,\"isMarried\":%s}".formatted(homeless, married)))
                .andExpect(status().isOk());
    }

    private void checkDetail(long id, long memberId, String expected) throws Exception {
        mvc.perform(get("/api/v1/housing/notices/{id}", id)
                        .header("Authorization", "Bearer " + tokens.createToken(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligibility.status").value(expected))
                .andExpect(jsonPath("$.data.eligibility.demo").value(true))
                .andExpect(jsonPath("$.data.eligibility.evaluatedOn").isNotEmpty());
    }
}
