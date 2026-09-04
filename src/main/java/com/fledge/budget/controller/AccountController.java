package com.fledge.budget.controller;

import com.fledge.budget.domain.AccountType;
import com.fledge.budget.dto.AccountListResponse;
import com.fledge.budget.dto.AccountSummaryResponse;
import com.fledge.budget.service.AccountService;
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

@Tag(name = "자산")
@RestController
@RequestMapping("/members/me/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @Operation(summary = "자산 요약", description = "입출금/예적금 계좌 합계와 순자산 총액")
    @GetMapping("/summary")
    public ApiResponse<AccountSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(accountService.getSummary(me.id()));
    }

    @Operation(summary = "계좌 목록", description = "account_type 별 계좌 목록과 합계. type=DEPOSIT 또는 SAVINGS")
    @GetMapping
    public ApiResponse<AccountListResponse> accounts(
            @AuthenticationPrincipal AuthenticatedMember me,
            @RequestParam String type
    ) {
        return ApiResponse.ok(accountService.getAccounts(me.id(), parseType(type)));
    }

    private AccountType parseType(String type) {
        try {
            return AccountType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "type 은 DEPOSIT 또는 SAVINGS 여야 합니다: " + type);
        }
    }
}