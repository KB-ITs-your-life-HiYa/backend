package com.fledge.budget.controller;

import com.fledge.budget.dto.BudgetChatAskRequest;
import com.fledge.budget.dto.BudgetChatAskResponse;
import com.fledge.budget.dto.BudgetChatSummaryResponse;
import com.fledge.budget.service.BudgetChatService;
import com.fledge.common.ApiResponse;
import com.fledge.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "생활비 챗봇")
@RestController
@RequestMapping("/members/me/budget/chat")
@RequiredArgsConstructor
public class BudgetChatController {

    private final BudgetChatService budgetChatService;

    @Operation(summary = "생활비 챗봇 요약 카드",
            description = "이번 달 지출·예산 진행률과 추천 질문을 함께 내려준다. 생활비 관리 탭 진입 시 첫 화면에 쓴다.")
    @GetMapping("/summary")
    public ApiResponse<BudgetChatSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(budgetChatService.summary(me.id()));
    }

    @Operation(summary = "생활비 자유질문",
            description = """
                    이 사용자의 실제 이번 달 지출·예산·고정비·저축·계좌 데이터를 근거로만 답한다.
                    지원금/독립지원/서비스 이용 질문은 여기가 아니라 /members/me/care/faq 로 보낸다.
                    범위 밖 질문이거나 근거가 부족하면 inScope=false 로 "답변이 어렵다"는 취지로 답한다.
                    """)
    @PostMapping("/ask")
    public ApiResponse<BudgetChatAskResponse> ask(
            @AuthenticationPrincipal AuthenticatedMember me,
            @Valid @RequestBody BudgetChatAskRequest request) {
        return ApiResponse.ok(budgetChatService.ask(me.id(), request.question()));
    }
}
