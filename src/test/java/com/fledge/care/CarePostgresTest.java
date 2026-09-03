package com.fledge.care;

import com.fledge.care.service.CareService;
import com.fledge.care.dto.CareDto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** 전용 임시 PostgreSQL(55432)에서만 실행. 기존 로컬/공유 DB 설정을 사용하지 않는다. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/postgres",
    "spring.datasource.username=care_test", "spring.datasource.password=",
    "spring.config.import=", "logging.level.org.hibernate.SQL=warn"
})
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "CARE_PG_TEST", matches = "true")
class CarePostgresTest {
    @Autowired CareService care;
    @Autowired JdbcTemplate jdbc;

    @Test void migrationsPersistenceResetAndSevenDayRecheck() {
        assertThat(care.resetDemo(2L).riskScore()).isZero();
        assertThat(care.setDemoDate(2L, LocalDate.of(2026, 9, 24)).riskScore()).isEqualTo(25);
        long savings = care.summary(2L).signals().getFirst().id();
        var reply = care.respond(2L, savings, new ButtonRequest(Choice.DIFFICULT, "pg-policy", null, null))
                .signals().getFirst().replies().getFirst();
        var card = new PolicyCard("2026001", "FINANCE", "DB 검증 정책", "지원 내용", "상시", "기관", "https://www.youthcenter.go.kr/");
        care.savePolicies(2L, savings, reply.id(), new Policies("READY", List.of(card)));
        assertThat(care.summary(2L).signals().getFirst().replies().getFirst().policies().cards()).containsExactly(card);
        assertThat(care.setDemoDate(2L, LocalDate.of(2026, 9, 26)).riskScore()).isEqualTo(65);
        long income = care.summary(2L).signals().getLast().id();
        assertThat(jdbc.queryForObject("select count(*) from referral_request where member_id=2", Integer.class)).isZero();
        care.requestReferral(2L, income); care.requestReferral(2L, income);
        assertThat(jdbc.queryForObject("select count(*) from referral_request where member_id=2", Integer.class)).isEqualTo(1);
        assertThat(care.summary(2L).signals().getLast().referral().status()).isEqualTo("REQUESTED");
        care.setDemoDate(2L, LocalDate.of(2026, 10, 1));
        assertThat(care.summary(2L).signals().getFirst().recheckedAt()).isNotNull();
        care.resetDemo(2L); care.resetDemo(2L);
        assertThat(jdbc.queryForObject("select count(*) from referral_request where member_id=2", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from transaction where member_id=2", Integer.class)).isEqualTo(83);
        care.setDemoDate(2L, LocalDate.of(2026, 9, 24));
        jdbc.update("insert into transaction(member_id,account_id,txn_date,txn_type,amount,merchant_name) values(2,11,'2026-09-24','EXPENSE',200000,'KB국민 시연 정기적금')");
        care.setDemoDate(2L, LocalDate.of(2026, 10, 1));
        var resolved = care.summary(2L).signals().getFirst();
        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(resolved.recheckedAt()).isNotNull();
        care.resetDemo(2L);
    }
}
