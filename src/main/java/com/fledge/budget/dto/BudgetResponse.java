package com.fledge.budget.dto;

import com.fledge.budget.domain.ExpenseCategory;
import com.fledge.budget.domain.MonthlyBudget;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record BudgetResponse(
        String month,
        Long totalAmount,
        List<CategoryBudget> categories
) {
    // 6개 카테고리를 항상 전부 내려준다. amount 가 null 이면 그 카테고리는 미설정.
    public static BudgetResponse of(YearMonth month, MonthlyBudget budget, Map<ExpenseCategory, Long> lastMonthAmounts) {
        List<CategoryBudget> categories = java.util.Arrays.stream(ExpenseCategory.values())
                .map(c -> new CategoryBudget(
                        c,
                        budget == null ? null : budget.amountOf(c).orElse(null),
                        lastMonthAmounts.getOrDefault(c, 0L)))
                .toList();
        return new BudgetResponse(
                month.toString(),
                budget == null ? null : budget.getTotalAmount(),
                categories
        );
    }

    public record CategoryBudget(
            ExpenseCategory category,
            Long amount,
            long lastMonthAmount
    ) {
    }
}