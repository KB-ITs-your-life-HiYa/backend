package com.fledge.budget.dto;

import com.fledge.budget.domain.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record SaveBudgetRequest(
        @NotBlank String month,
        @NotNull @Positive Long totalAmount,
        @NotNull List<CategoryAmount> categories
) {
    // amount 는 0/음수 판정을 서비스에서 한다 (0 = 미설정 취급, 음수만 오류).
    public record CategoryAmount(
            @NotNull ExpenseCategory category,
            @NotNull Long amount
    ) {
    }
}