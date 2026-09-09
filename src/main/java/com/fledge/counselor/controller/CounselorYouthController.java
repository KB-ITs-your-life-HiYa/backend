package com.fledge.counselor.controller;

import com.fledge.common.ApiResponse;
import com.fledge.counselor.dto.CounselorYouthResponse;
import com.fledge.counselor.service.CounselorYouthService;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "상담사")
@RestController
@RequestMapping("/members/me/counselor/youths")
@RequiredArgsConstructor
public class CounselorYouthController {
    private final CounselorYouthService service;

    @GetMapping
    @Operation(summary = "내 담당 청년 목록 조회")
    public ApiResponse<List<CounselorYouthResponse>> findMine(
            @AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(service.findMine(me.id()));
    }
}
