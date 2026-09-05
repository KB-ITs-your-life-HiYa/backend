package com.fledge.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record SurveyRequest(
        @Schema(description = "가구원 수 (본인 포함, 1~10)")
        @Min(value = 1, message = "가구원 수는 1명 이상이어야 합니다")
        @Max(value = 10, message = "가구원 수는 10명 이하로 입력해주세요")
        Integer householdSize,

        @Schema(description = "기준중위소득 구간(%). 32/48/50/60/100/120/150/999(초과). 모르면 null")
        Integer incomePctBracket,

        @Schema(description = "기초생활수급자 등 여부")
        Boolean isBenefitRecipient,

        @Schema(description = "재직 상태. JOB_SEEKER 는 졸업(중퇴) 후 2년 이내 구직중")
        @Pattern(regexp = "EMPLOYED|SELF_EMPLOYED|STUDENT|JOB_SEEKER|UNEMPLOYED",
                message = "올바르지 않은 재직 상태입니다")
        String employmentStatus,

        @Schema(description = "주거 형태")
        @Pattern(regexp = "OWNED|JEONSE|MONTHLY_RENT|FREE|SELF_RELIANCE_HOUSE|PUBLIC_RENTAL",
                message = "올바르지 않은 주거 형태입니다")
        String housingType,

        @Schema(description = "해당하는 특성 태그 목록")
        List<String> tags
) {
}