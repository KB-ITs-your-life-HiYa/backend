package com.fledge.budget.controller;

import com.fledge.budget.dto.BudgetResponse;
import com.fledge.budget.dto.SaveBudgetRequest;
import com.fledge.budget.service.BudgetService;
import com.fledge.common.ApiResponse;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Tag(name = "생활비 예산")
@RestController
@RequestMapping("/members/me/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "예산 조회", description = "month=YYYY-MM. 생략 시 이번 달. 카테고리별 지난달 사용액을 함께 내려준다.")
    @GetMapping
    public ApiResponse<BudgetResponse> get(
            @AuthenticationPrincipal AuthenticatedMember me,
            @RequestParam(required = false) String month) {
        return ApiResponse.ok(budgetService.get(me.id(), parseMonth(month)));
    }

    @Operation(summary = "예산 저장", description = "해당 월 예산을 통째로 갈아끼운다 (upsert). 지난 달을 포함해 어느 달이든 수정 가능.")
    @PutMapping
    public ApiResponse<BudgetResponse> save(
            @AuthenticationPrincipal AuthenticatedMember me,
            @Valid @RequestBody SaveBudgetRequest request) {
        return ApiResponse.ok(budgetService.save(me.id(), parseMonth(request.month()), request));
    }

    @Operation(summary = "예산 삭제", description = "month=YYYY-MM. 총예산을 지우면 카테고리 예산도 함께 지워진다.")
    @DeleteMapping
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedMember me,
            @RequestParam String month) {
        budgetService.delete(me.id(), parseMonth(month));
        return ApiResponse.ok(null);
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "month 는 YYYY-MM 형식이어야 합니다: " + month);
        }
    }
}