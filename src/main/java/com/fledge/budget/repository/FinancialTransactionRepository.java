package com.fledge.budget.repository;

import com.fledge.budget.domain.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    java.util.List<FinancialTransaction> findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(
            Long memberId, java.time.LocalDate from, java.time.LocalDate to);
}
