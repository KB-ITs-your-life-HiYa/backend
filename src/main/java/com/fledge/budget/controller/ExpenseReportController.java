package com.fledge.budget.controller;

import com.fledge.budget.dto.ExpenseReportResponse;
import com.fledge.budget.dto.ExpenseSummaryResponse;
import com.fledge.budget.service.ExpenseReportService;
import com.fledge.common.ApiResponse;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Tag(name = "지출 리포트")
@RestController
@RequestMapping("/members/me")
@RequiredArgsConstructor
public class ExpenseReportController {
    private final ExpenseReportService expenseReportService;

    @Operation(summary = "지출 요약", description = "이번 달 총 지출과 지난달 같은 기간 대비")
    @GetMapping("/expense-summary")
    public ApiResponse<ExpenseSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(expenseReportService.getSummary(me.id()));
    }

    @Operation(summary = "월별 지출 리포트", description = "month=YYYY-MM. 생략 시 이번 달")
    @GetMapping("/expense-report")
    public ApiResponse<ExpenseReportResponse> report(
            @AuthenticationPrincipal AuthenticatedMember me,
            @RequestParam(required = false) String month
    ) {
        return ApiResponse.ok(expenseReportService.getReport(me.id(), parseMonth(month)));
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