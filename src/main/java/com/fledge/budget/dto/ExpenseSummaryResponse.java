package com.fledge.budget.dto;

public record ExpenseSummaryResponse(
        long currentMonthTotal,
        long lastMonthSamePeriodTotal,
        long difference
) {
    public static ExpenseSummaryResponse of(long currentMonthTotal, long lastMonthSamePeriodTotal) {
        return new ExpenseSummaryResponse(
                currentMonthTotal, lastMonthSamePeriodTotal, currentMonthTotal - lastMonthSamePeriodTotal);
    }
}