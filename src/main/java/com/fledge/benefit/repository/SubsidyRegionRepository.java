package com.fledge.benefit.repository;

import com.fledge.benefit.domain.SubsidyRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubsidyRegionRepository extends JpaRepository<SubsidyRegion, Long> {
    List<SubsidyRegion> findBySubsidy_Id(Long subsidyId);
}
