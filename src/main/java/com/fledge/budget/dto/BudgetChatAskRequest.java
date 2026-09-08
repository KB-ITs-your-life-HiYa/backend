package com.fledge.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 생활비 챗봇 자유질문. Care RAG(CareDto.FaqAskRequest)와는 별개 경로 — 이쪽은 문서가 아니라
// 이 사용자의 실제 지출/예산/고정비/저축 데이터를 근거로 답한다.
public record BudgetChatAskRequest(@NotBlank @Size(max = 300) String question) {
}
