package com.fledge.care.repository;

import com.fledge.care.domain.CareSignal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareSignalRepository extends JpaRepository<CareSignal, Long> {
    java.util.List<CareSignal> findByMemberIdOrderByDetectedAtAscIdAsc(Long memberId);
    java.util.Optional<CareSignal> findByIdAndMemberId(Long id, Long memberId);
    java.util.Optional<CareSignal> findByMoneyCycleIdAndStatus(Long cycleId, String status);
}
