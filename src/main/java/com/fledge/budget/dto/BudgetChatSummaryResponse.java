package com.fledge.budget.dto;

import java.util.List;

// 생활비 챗봇 탭 진입 시 첫 화면에 쓰는 요약 카드. 이번 달 지출/예산 진행률과 추천 질문을 함께 내려준다.
public record BudgetChatSummaryResponse(
        String month,
        long totalExpense,
        Long totalBudget,
        Long remaining,
        Integer progressRatio,
        String greeting,
        List<String> quickQuestions
) {
}
