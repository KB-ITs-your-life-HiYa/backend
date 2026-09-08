package com.fledge.member.controller;

import com.fledge.common.ApiResponse;
import com.fledge.member.dto.AddMemberSubsidyRequest;
import com.fledge.benefit.dto.SubsidySummaryResponse;
import com.fledge.member.service.MemberSubsidyService;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "받고 있는 지원금")
@RestController
@RequestMapping("/members/me/subsidies")
@RequiredArgsConstructor
public class MemberSubsidyController {

    private final MemberSubsidyService memberSubsidyService;

    @Operation(summary = "받고 있는 지원금 목록 조회")
    @GetMapping
    public ApiResponse<List<SubsidySummaryResponse>> mySubsidies(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(memberSubsidyService.findMine(me.id()));
    }

    @Operation(summary = "받고 있는 지원금 추가", description = "이미 추가돼 있으면 아무 일도 하지 않는다")
    @PostMapping
    public ApiResponse<Void> addSubsidy(@AuthenticationPrincipal AuthenticatedMember me,
                                        @Valid @RequestBody AddMemberSubsidyRequest request) {
        memberSubsidyService.add(me.id(), request.subsidyId());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "받고 있는 지원금 삭제")
    @DeleteMapping("/{subsidyId}")
    public ApiResponse<Void> removeSubsidy(@AuthenticationPrincipal AuthenticatedMember me,
                                           @PathVariable Long subsidyId) {
        memberSubsidyService.remove(me.id(), subsidyId);
        return ApiResponse.ok(null);
    }
}
