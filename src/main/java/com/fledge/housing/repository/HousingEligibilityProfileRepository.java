package com.fledge.housing.repository;

import com.fledge.housing.domain.HousingEligibilityProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HousingEligibilityProfileRepository extends JpaRepository<HousingEligibilityProfile, Long> {
}
