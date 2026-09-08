package com.fledge.housing.dto;

import com.fledge.housing.domain.HousingEligibilityProfile;
import com.fledge.housing.domain.YouthPurchasePriorityBasis;

import java.time.OffsetDateTime;

public record HousingEligibilityProfileResponse(
        boolean isHomeless,
        boolean isMarried,
        YouthPurchasePriorityBasis youthPurchasePriorityBasis,
        OffsetDateTime updatedAt
) {
    public static HousingEligibilityProfileResponse from(HousingEligibilityProfile profile) {
        return new HousingEligibilityProfileResponse(
                profile.isHomeless(), profile.isMarried(), profile.getYouthPurchasePriorityBasis(),
                profile.getUpdatedAt());
    }
}
