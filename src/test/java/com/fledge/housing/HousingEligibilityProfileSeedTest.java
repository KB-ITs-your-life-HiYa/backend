package com.fledge.housing;

import com.fledge.housing.repository.HousingEligibilityProfileRepository;
import com.fledge.housing.domain.YouthPurchasePriorityBasis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Sql("/db/seed/R__seed_01_member.sql")
class HousingEligibilityProfileSeedTest {
    @Autowired DataSource dataSource;
    @Autowired HousingEligibilityProfileRepository profiles;

    private void runSeed() {
        new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/R__seed_08_housing_eligibility_profile.sql"))
                .execute(dataSource);
    }

    @Test
    void 데모회원은_무주택_미혼으로_최초_설정한다() {
        profiles.deleteById(1L);
        profiles.deleteById(2L);
        profiles.flush();

        runSeed();

        for (long id : new long[]{1L, 2L}) {
            var profile = profiles.findById(id).orElseThrow();
            assertThat(profile.isHomeless()).isTrue();
            assertThat(profile.isMarried()).isFalse();
        }
        assertThat(profiles.findById(2L).orElseThrow().getYouthPurchasePriorityBasis())
                .isEqualTo(YouthPurchasePriorityBasis.BENEFIT_RECIPIENT);
    }

    @Test
    void 시드_재실행은_수정한_정보를_덮어쓰지_않는다() {
        runSeed();
        var profile = profiles.findById(2L).orElseThrow();
        profile.update(false, true);
        profile.updateYouthPurchasePriorityBasis(YouthPurchasePriorityBasis.NONE);
        profiles.flush();

        runSeed();

        var saved = profiles.findById(2L).orElseThrow();
        assertThat(saved.isHomeless()).isFalse();
        assertThat(saved.isMarried()).isTrue();
        assertThat(saved.getYouthPurchasePriorityBasis()).isEqualTo(YouthPurchasePriorityBasis.NONE);
    }
}
