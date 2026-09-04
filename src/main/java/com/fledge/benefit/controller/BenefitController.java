package com.fledge.benefit.controller;

import com.fledge.benefit.dto.CategoryMatchResponse;
import com.fledge.benefit.service.BenefitMatchingService;
import com.fledge.common.ApiResponse;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "지원금 매칭")
@RestController
@RequestMapping("/members/me/benefit")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitMatchingService benefitMatchingService;

    @Operation(summary = "지원금 매칭 조회",
            description = "회원 정보·설문을 바탕으로 매칭되는 지원금을 카테고리별로 반환한다")
    @GetMapping("/matches")
    public ApiResponse<List<CategoryMatchResponse>> matches(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(benefitMatchingService.getMatches(me.id()));
    }
}