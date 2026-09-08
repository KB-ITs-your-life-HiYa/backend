package com.fledge.housing.dto;

import com.fledge.housing.domain.HousingEligibilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "저장된 회원 정보에 따른 참고 판정. 기관의 최종 심사 결과가 아니다")
public record HousingEligibilityResponse(
        HousingEligibilityStatus status,
        List<String> reasons,
        List<String> missingFields,
        LocalDate evaluatedOn,
        @Schema(description = "2025년 조건을 적용한 데모 공고 판정이면 true")
        boolean demo
) {
}
