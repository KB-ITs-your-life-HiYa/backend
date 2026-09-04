package com.fledge.member.controller;

import com.fledge.common.ApiResponse;
import com.fledge.member.dto.MemberResponse;
import com.fledge.member.service.MemberService;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fledge.member.dto.SurveyRequest;
import com.fledge.member.dto.SurveyResponse;
import com.fledge.member.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "회원")
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final SurveyService surveyService;

    @Operation(summary = "내 정보 조회", description = "Authorization 헤더의 토큰으로 본인을 식별한다")
    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(memberService.findMe(me.id()));
    }

    @Operation(summary = "정책 매칭 설문 저장",
            description = "가구·소득·재직·주거 등 매칭에 필요한 추가 정보를 저장한다. 이미 저장된 값이 있으면 덮어쓴다")
    @PostMapping("/me/survey")
    public ApiResponse<SurveyResponse> saveSurvey(@AuthenticationPrincipal AuthenticatedMember me,
                                                  @Valid @RequestBody SurveyRequest request) {
        return ApiResponse.ok(surveyService.save(me.id(), request));
    }
}
