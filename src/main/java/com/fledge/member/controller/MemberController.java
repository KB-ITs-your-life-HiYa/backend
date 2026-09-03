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

@Tag(name = "회원")
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "Authorization 헤더의 토큰으로 본인을 식별한다")
    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(memberService.findMe(me.id()));
    }
}
