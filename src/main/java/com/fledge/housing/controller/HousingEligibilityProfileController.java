package com.fledge.housing.controller;

import com.fledge.common.ApiResponse;
import com.fledge.housing.dto.HousingEligibilityProfileRequest;
import com.fledge.housing.dto.HousingEligibilityProfileResponse;
import com.fledge.housing.service.HousingEligibilityProfileService;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "주거 자격 정보")
@RestController
@RequestMapping("/members/me/housing-eligibility")
@RequiredArgsConstructor
public class HousingEligibilityProfileController {
    private final HousingEligibilityProfileService service;

    @Operation(summary = "내 주거 자격 정보 조회", description = "미입력 회원은 data가 null이다. 공고와 관계없이 회원별로 재사용한다")
    @GetMapping
    public ApiResponse<HousingEligibilityProfileResponse> findMe(
            @AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(service.findMe(me.id()));
    }

    @Operation(summary = "내 주거 자격 정보 저장", description = "무주택·혼인 여부를 모두 입력한다. 기존 정보가 있으면 수정하며 미입력 상태를 임의로 채우지 않는다")
    @PutMapping
    public ApiResponse<HousingEligibilityProfileResponse> save(
            @AuthenticationPrincipal AuthenticatedMember me,
            @Valid @RequestBody HousingEligibilityProfileRequest request) {
        return ApiResponse.ok(service.save(me.id(), request));
    }
}
