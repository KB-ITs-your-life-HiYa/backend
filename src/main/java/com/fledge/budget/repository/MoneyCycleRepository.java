package com.fledge.budget.repository;

import com.fledge.budget.domain.MoneyCycle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoneyCycleRepository extends JpaRepository<MoneyCycle, Long> {
    java.util.List<MoneyCycle> findByMemberIdOrderByExpectedDateAscIdAsc(Long memberId);
    java.util.Optional<MoneyCycle> findByScheduleIdAndCycleMonth(Long scheduleId, java.time.LocalDate month);
    java.util.Optional<MoneyCycle> findByIdAndMemberId(Long id, Long memberId);
}
