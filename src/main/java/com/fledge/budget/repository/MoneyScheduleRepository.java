package com.fledge.budget.repository;

import com.fledge.budget.domain.MoneySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoneyScheduleRepository extends JpaRepository<MoneySchedule, Long> {
    java.util.List<MoneySchedule> findByMemberIdAndIsActiveTrueOrderById(Long memberId);
    java.util.Optional<MoneySchedule> findByIdAndMemberId(Long id, Long memberId);
}
