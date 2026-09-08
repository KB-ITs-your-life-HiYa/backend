package com.fledge.member.dto;

import jakarta.validation.constraints.NotNull;

public record AddMemberSubsidyRequest(
        @NotNull(message = "subsidyId는 필수입니다")
        Long subsidyId
) {
}
