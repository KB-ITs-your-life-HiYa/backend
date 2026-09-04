package com.fledge.budget.dto;

import com.fledge.budget.domain.ExpenseCategory;

import java.util.List;

public record ExpenseReportResponse(
        String month,
        Summary summary,
        MonthlyTrend monthlyTrend,
        List<CategoryBreakdown> categories,
        Navigation navigation,
        Long monthlyBudget
) {
    public record Summary(long totalExpense, long totalIncome) {
    }

    public record MonthlyTrend(List<MonthPoint> months, long averageExpense) {
        public record MonthPoint(String month, long totalExpense) {
        }
    }

    public record CategoryBreakdown(
            ExpenseCategory category,
            long currentAmount,
            long previousAmount,
            long difference,
            int progressRatio,
            Long budget
    ) {
    }

    public record Navigation(boolean hasPrevious, boolean hasNext) {
    }
}