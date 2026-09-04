package com.fledge.housing.controller;

import com.fledge.common.ApiResponse;
import com.fledge.housing.dto.HousingCalendarResponse;
import com.fledge.housing.service.HousingCalendarService;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "독립지원")
@RestController
@RequestMapping("/housing")
@RequiredArgsConstructor
public class HousingController {
    private final HousingCalendarService calendarService;

    @Operation(summary = "캘린더 공고 조회",
            description = """
                    그 달에 접수기간이 걸치는 공고를 돌려준다.
                    regionCode 를 안 주면 로그인한 회원의 거주 시/도로 거른다.
                    regionCode=ALL 이면 전국을 보여준다.
                    """)
    @GetMapping("/calendar")
    public ApiResponse<HousingCalendarResponse> calendar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String regionCode,
            @AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(calendarService.findByMonth(year, month, me.id(), regionCode));
    }
}
