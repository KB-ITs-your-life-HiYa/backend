package com.fledge.housing.controller;

import com.fledge.common.ApiResponse;
import com.fledge.housing.collect.HousingCollector;
import com.fledge.housing.service.HousingAdminService;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상담사 전용 수동 수집 트리거.
 *
 * 스케줄러는 로컬에서 꺼져 있으므로, 데이터가 필요할 때 이것으로 한 번 돌린다.
 */
@Tag(name = "독립지원")
@RestController
@RequestMapping("/housing/admin")
@RequiredArgsConstructor
public class HousingAdminController {

    private final HousingAdminService housingAdminService;

    @Operation(summary = "공고 수집 실행",
            description = "상담사만 마이홈포털에서 공고를 가져와 저장할 수 있다")
    @PostMapping("/collect")
    public ApiResponse<HousingCollector.CollectResult> collect(
            @AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(housingAdminService.collect(me.id()));
    }
}
