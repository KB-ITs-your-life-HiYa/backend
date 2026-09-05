package com.fledge.budget.dto;

import com.fledge.budget.domain.ExpenseCategory;

import java.util.List;

public record SupportEndForecastResponse(
        boolean eligible,
        Forecast forecast
) {
    public static SupportEndForecastResponse notEligible() {
        return new SupportEndForecastResponse(false, null);
    }

    public record Forecast(
            long daysUntilSupportEnd,
            long monthsUntilSupportEnd,
            int monthsUsedForAverage,
            boolean dataAvailable,
            Long monthlyShortfall,
            long currentSavingsTotal,
            Long savingsRunwayMonths,
            Reduction reduction
    ) {
    }

    public record Reduction(
            List<CategoryReduction> categories,
            long totalMonthlySavings,
            long totalSavingsByEnd,
            Long improvedRunwayMonths
    ) {
    }

    public record CategoryReduction(
            ExpenseCategory category,
            long averageAmount,
            long reducedAmount,
            long monthlySavings
    ) {
    }
}