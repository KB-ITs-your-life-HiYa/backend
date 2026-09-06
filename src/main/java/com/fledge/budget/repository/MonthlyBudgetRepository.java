package com.fledge.budget.repository;

import com.fledge.budget.domain.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {

    Optional<MonthlyBudget> findByMemberIdAndBudgetMonth(Long memberId, LocalDate budgetMonth);
}