package com.fledge.budget.service;

import com.fledge.budget.domain.ExpenseCategory;
import com.fledge.budget.domain.FinancialTransaction;

import java.time.LocalDate;
import java.util.List;

// 거래 목록에서 기간별 합계를 뽑는 공용 계산. ExpenseReportService, SupportEndForecastService 가 함께 쓴다.
public final class TransactionAggregator {
    private TransactionAggregator() {
    }

    public static long sumExpense(List<FinancialTransaction> txns, LocalDate from, LocalDate to) {
        return txns.stream()
                .filter(t -> "EXPENSE".equals(t.getTxnType()))
                .filter(t -> !"SAVINGS".equals(t.getCategory()))
                .filter(t -> inRange(t, from, to))
                .mapToLong(FinancialTransaction::getAmount)
                .sum();
    }

    public static long sumIncome(List<FinancialTransaction> txns, LocalDate from, LocalDate to) {
        return txns.stream()
                .filter(t -> "INCOME".equals(t.getTxnType()))
                .filter(t -> inRange(t, from, to))
                .mapToLong(FinancialTransaction::getAmount)
                .sum();
    }

    public static long sumCategory(List<FinancialTransaction> txns, ExpenseCategory category, LocalDate from, LocalDate to) {
        return txns.stream()
                .filter(t -> "EXPENSE".equals(t.getTxnType()))
                .filter(t -> category.name().equals(t.getCategory()))
                .filter(t -> inRange(t, from, to))
                .mapToLong(FinancialTransaction::getAmount)
                .sum();
    }

    public static boolean inRange(FinancialTransaction t, LocalDate from, LocalDate to) {
        return !t.getTxnDate().isBefore(from) && !t.getTxnDate().isAfter(to);
    }
}