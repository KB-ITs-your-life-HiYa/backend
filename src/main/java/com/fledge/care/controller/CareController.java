package com.fledge.care.controller;

import com.fledge.care.dto.CareDto.*;
import com.fledge.care.service.CareService;
import com.fledge.common.ApiResponse;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members/me/care")
public class CareController {
    private final CareService care;
    private final com.fledge.care.service.CarePolicyService policies;
    private final com.fledge.care.service.CareGeminiService gemini;

    @GetMapping
    @Operation(summary = "내 케어 상태와 상담 이력 조회")
    public ApiResponse<Summary> summary(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(care.summary(me.id()));
    }

    @PostMapping("/evaluate")
    @Operation(summary = "기준일까지의 금융 거래를 확인하고 케어 상태 갱신")
    public ApiResponse<Summary> evaluate(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(care.evaluate(me.id()));
    }

    @PostMapping("/signals/{signalId}/responses")
    @Operation(summary = "버튼 상담 응답 및 일정 변경")
    public ApiResponse<Summary> respond(@AuthenticationPrincipal AuthenticatedMember me,
                                       @PathVariable Long signalId, @Valid @RequestBody ButtonRequest request) {
        return ApiResponse.ok(care.respond(me.id(), signalId, request));
    }

    @PostMapping("/signals/{signalId}/messages")
    @Operation(summary = "Gemini 직접입력 상담")
    public ApiResponse<Summary> message(@AuthenticationPrincipal AuthenticatedMember me,
                                       @PathVariable Long signalId,
                                       @Valid @RequestBody FreeTextRequest request) {
        return ApiResponse.ok(gemini.message(me.id(), signalId, request));
    }

    @PostMapping("/signals/{signalId}/responses/{responseId}/gemini")
    @Operation(summary = "실패한 Gemini 답변 재시도")
    public ApiResponse<Summary> retryGemini(@AuthenticationPrincipal AuthenticatedMember me,
                                           @PathVariable Long signalId,
                                           @PathVariable Long responseId) {
        return ApiResponse.ok(gemini.retry(me.id(), signalId, responseId));
    }
    @PostMapping("/signals/{signalId}/responses/{responseId}/policies")
    @Operation(summary = "상담에 맞는 온통청년 정책 조회 또는 재시도")
    public ApiResponse<Summary> policies(@AuthenticationPrincipal AuthenticatedMember me,
                                        @PathVariable Long signalId, @PathVariable Long responseId) {
        return ApiResponse.ok(policies.load(me.id(), signalId, responseId));
    }

    @PostMapping("/signals/{signalId}/referrals")
    @Operation(summary = "사용자 동의 후 담당자 연결 요청 접수")
    public ApiResponse<Summary> refer(@AuthenticationPrincipal AuthenticatedMember me,
                                     @PathVariable Long signalId, @Valid @RequestBody ReferralConsent request) {
        return ApiResponse.ok(care.requestReferral(me.id(), signalId));
    }
}
