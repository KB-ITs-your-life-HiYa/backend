package com.fledge.counselor.controller;

import com.fledge.common.ApiResponse;
import com.fledge.counselor.dto.CounselorReferralResponse;
import com.fledge.counselor.service.CounselorReferralService;
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
@RequestMapping("/members/me/counselor/referrals")
@RequiredArgsConstructor
public class CounselorReferralController {
    private final CounselorReferralService service;

    @GetMapping
    @Operation(summary = "내게 전달된 상담 요청 조회")
    public ApiResponse<List<CounselorReferralResponse>> findMine(
            @AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(service.findMine(me.id()));
    }
}
