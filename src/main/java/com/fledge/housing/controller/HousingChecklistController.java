package com.fledge.housing.controller;

import com.fledge.common.ApiResponse;
import com.fledge.housing.dto.CreateHousingChecklistItemRequest;
import com.fledge.housing.dto.CreateHousingChecklistRequest;
import com.fledge.housing.dto.HousingChecklistResponse;
import com.fledge.housing.dto.UpdateHousingChecklistItemRequest;
import com.fledge.housing.service.HousingChecklistService;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "독립지원 체크리스트")
@RestController
@RequestMapping("/members/me/housing/checklists")
@RequiredArgsConstructor
public class HousingChecklistController {

    private final HousingChecklistService checklistService;

    @Operation(summary = "내 체크리스트 목록", description = "진행률(doneCount/totalCount/progress)과 항목을 함께 돌려준다.")
    @GetMapping
    public ApiResponse<List<HousingChecklistResponse>> list(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(checklistService.list(me.id()));
    }

    @Operation(summary = "템플릿으로 체크리스트 생성",
            description = "HOUSE_HUNTING / MOVE_IN / MOVING 중 하나. 종류당 1개만 만들 수 있다.")
    @PostMapping
    public ApiResponse<HousingChecklistResponse> create(
            @AuthenticationPrincipal AuthenticatedMember me,
            @Valid @RequestBody CreateHousingChecklistRequest request) {
        return ApiResponse.ok(checklistService.create(me.id(), request));
    }

    @Operation(summary = "체크리스트 삭제")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedMember me,
            @PathVariable Long id) {
        checklistService.delete(me.id(), id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "항목 추가")
    @PostMapping("/{id}/items")
    public ApiResponse<HousingChecklistResponse.HousingChecklistItemResponse> addItem(
            @AuthenticationPrincipal AuthenticatedMember me,
            @PathVariable Long id,
            @Valid @RequestBody CreateHousingChecklistItemRequest request) {
        return ApiResponse.ok(checklistService.addItem(me.id(), id, request));
    }

    @Operation(summary = "항목 수정", description = "전달한 필드만 반영. dueDate·memo 는 null 로 지울 수 있다.")
    @PatchMapping("/{id}/items/{itemId}")
    public ApiResponse<HousingChecklistResponse.HousingChecklistItemResponse> updateItem(
            @AuthenticationPrincipal AuthenticatedMember me,
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateHousingChecklistItemRequest request) {
        return ApiResponse.ok(checklistService.updateItem(me.id(), id, itemId, request));
    }

    @Operation(summary = "항목 삭제")
    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> deleteItem(
            @AuthenticationPrincipal AuthenticatedMember me,
            @PathVariable Long id,
            @PathVariable Long itemId) {
        checklistService.deleteItem(me.id(), id, itemId);
        return ApiResponse.ok(null);
    }
}
