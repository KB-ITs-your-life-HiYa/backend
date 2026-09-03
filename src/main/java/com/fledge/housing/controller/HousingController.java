package com.fledge.housing.controller;

import com.fledge.common.ApiResponse;
import com.fledge.housing.dto.HousingNoticeSummary;
import com.fledge.housing.service.HousingCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "독립지원")
@RestController
@RequestMapping("/housing")
@RequiredArgsConstructor
public class HousingController {
    private final HousingCalendarService calendarService;

    @Operation(summary = "캘린더 공고 조회",
            description = "그 달에 접수기간이 걸치는 공고를 돌려준다. 그 달에 시작하는 공고만이 아니다")
    @GetMapping("/calendar")
    public ApiResponse<List<HousingNoticeSummary>> calendar(
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.ok(calendarService.findByMonth(year, month));
    }
}
