package com.fledge.housing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record HousingEligibilityProfileRequest(
        @Schema(description = "본인 명의 주택을 소유하지 않으면 true. 현재 거주 형태와는 별개")
        @NotNull(message = "본인 무주택 여부를 선택해주세요")
        Boolean isHomeless,
        @Schema(description = "현재 혼인 중이면 true")
        @NotNull(message = "혼인 여부를 선택해주세요")
        Boolean isMarried
) {
}
