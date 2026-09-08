package com.fledge.housing.dto;

import com.fledge.housing.domain.HousingEligibilityProfile;

import java.time.OffsetDateTime;

public record HousingEligibilityProfileResponse(
        boolean isHomeless,
        boolean isMarried,
        OffsetDateTime updatedAt
) {
    public static HousingEligibilityProfileResponse from(HousingEligibilityProfile profile) {
        return new HousingEligibilityProfileResponse(
                profile.isHomeless(), profile.isMarried(), profile.getUpdatedAt());
    }
}
