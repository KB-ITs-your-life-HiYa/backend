package com.fledge.benefit.controller;

import com.fledge.benefit.dto.SubsidySummaryResponse;
import com.fledge.benefit.service.SubsidyService;
import com.fledge.common.ApiResponse;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "지원금")
@RestController
@RequestMapping("/subsidies")
@RequiredArgsConstructor
public class SubsidyController {

    private final SubsidyService subsidyService;

    @Operation(summary = "지원금 이름 검색", description = "\"받고 있는 지원금\" 추가 화면에서 쓴다. 우리 카탈로그 안에서만 검색되며, 회원의 현재 지역과 일치하는 결과가 위로 온다")
    @GetMapping
    public ApiResponse<List<SubsidySummaryResponse>> search(@RequestParam String q,
                                                              @AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(subsidyService.search(q, me.id()));
    }
}
