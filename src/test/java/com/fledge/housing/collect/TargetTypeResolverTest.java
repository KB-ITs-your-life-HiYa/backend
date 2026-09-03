package com.fledge.housing.collect;

import com.fledge.housing.domain.TargetType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TargetTypeResolverTest {

    @Test
    void 자립준비청년_공고를_가려낸다() {
        assertThat(TargetTypeResolver.resolve("2026년 자립준비청년 전세임대 입주자 모집"))
                .isEqualTo(TargetType.SELF_RELIANCE);
        assertThat(TargetTypeResolver.resolve("보호종료아동 매입임대주택 모집 공고"))
                .isEqualTo(TargetType.SELF_RELIANCE);
    }

    @Test
    void 청년_공고를_가려낸다() {
        assertThat(TargetTypeResolver.resolve("2026년 청년 전세임대 1순위 입주자 수시모집"))
                .isEqualTo(TargetType.YOUTH);
        assertThat(TargetTypeResolver.resolve("기숙사형 청년주택(개봉동) 상시 입사생 모집"))
                .isEqualTo(TargetType.YOUTH);
    }

    @Test
    void 자립준비청년이_청년보다_우선한다() {
        // 두 키워드가 같이 있으면 더 좁은 쪽으로 분류해야 캘린더 1순위에 올라간다
        assertThat(TargetTypeResolver.resolve("자립준비청년 대상 청년주택 모집"))
                .isEqualTo(TargetType.SELF_RELIANCE);
    }

    @Test
    void 그_외는_일반이다() {
        assertThat(TargetTypeResolver.resolve("익산시 국민임대주택 예비입주자 모집 공고"))
                .isEqualTo(TargetType.GENERAL);
        assertThat(TargetTypeResolver.resolve("2026 다자녀 전세임대 입주자 수시모집 공고"))
                .isEqualTo(TargetType.GENERAL);
    }

    @Test
    void 공고명이_없어도_터지지_않는다() {
        assertThat(TargetTypeResolver.resolve(null)).isEqualTo(TargetType.GENERAL);
        assertThat(TargetTypeResolver.resolve("")).isEqualTo(TargetType.GENERAL);
    }
}
