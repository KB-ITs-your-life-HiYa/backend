package com.fledge.budget.controller;

import com.fledge.budget.dto.SupportEndForecastResponse;
import com.fledge.budget.service.SupportEndForecastService;
import com.fledge.common.ApiResponse;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "수당 종료 대비")
@RestController
@RequestMapping("/members/me")
@RequiredArgsConstructor
public class SupportEndForecastController {
    private final SupportEndForecastService supportEndForecastService;

    @Operation(
            summary = "수당 종료 대비 계산",
            description = "수당 종료까지 1년 이내인 회원에게만 계산 결과를 내려준다. 그 외에는 eligible=false, forecast=null"
    )
    @GetMapping("/support-end-forecast")
    public ApiResponse<SupportEndForecastResponse> forecast(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(supportEndForecastService.getForecast(me.id()));
    }
}