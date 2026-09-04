package com.fledge.benefit.repository;

import com.fledge.benefit.domain.SubsidyRaw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubsidyRawRepository extends JpaRepository<SubsidyRaw, Long> {
    Optional<SubsidyRaw> findBySourceAndExternalId(String source, String externalId);
}