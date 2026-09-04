package com.fledge.benefit.repository;

import com.fledge.benefit.domain.SubsidyBenefit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubsidyBenefitRepository extends JpaRepository<SubsidyBenefit, Long> {
    List<SubsidyBenefit> findBySubsidy_Id(Long subsidyId);
}