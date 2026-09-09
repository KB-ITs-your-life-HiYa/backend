package com.fledge.member;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Sql("/db/seed/R__seed_01_member.sql")
class CounselorAssignmentSeedTest {
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    private void runSeed() {
        new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/R__seed_09_counselor_assignment.sql"))
                .execute(dataSource);
    }

    @Test
    void 상담사에게_두_데모청년을_배정한다() {
        runSeed();

        Integer count = jdbc.queryForObject("""
                SELECT count(*)
                FROM counselor_youth_assignment a
                JOIN counselor c ON c.id = a.counselor_id
                JOIN member counselor_member ON counselor_member.id = c.member_id
                WHERE counselor_member.email = 'counselor@fledge.local'
                  AND a.unassigned_at IS NULL
                """, Integer.class);

        assertThat(count).isEqualTo(2);
        assertThat(jdbc.queryForList("""
                SELECT youth.name
                FROM counselor_youth_assignment a
                JOIN member youth ON youth.id = a.youth_member_id
                WHERE a.unassigned_at IS NULL
                ORDER BY youth.id
                """, String.class)).containsExactly("김도윤", "이서윤");
    }

    @Test
    void 시드를_다시_실행해도_활성배정이_중복되지_않는다() {
        runSeed();
        runSeed();

        Integer count = jdbc.queryForObject("""
                SELECT count(*)
                FROM counselor_youth_assignment
                WHERE unassigned_at IS NULL
                """, Integer.class);

        assertThat(count).isEqualTo(2);
    }
}
