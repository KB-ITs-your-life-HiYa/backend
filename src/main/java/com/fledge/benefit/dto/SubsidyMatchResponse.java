package com.fledge.benefit.dto;

import com.fledge.benefit.domain.SubsidyBenefit;

import java.time.LocalDate;
import java.util.List;

public record SubsidyMatchResponse(
        Long subsidyId,
        String name,
        String summary,
        String orgName,
        String category,
        String applyMethod,
        String applyDeadlineRaw,
        LocalDate applyDeadlineDate,
        String detailUrl,
        List<BenefitItem> benefits,
        List<MatchCondition> conditions,
        long needsReviewCount
) {
    public record BenefitItem(String benefitName, Long amountKrw, String cycle) {
        public static BenefitItem from(SubsidyBenefit b) {
            return new BenefitItem(b.getBenefitName(), b.getAmountKrw(), b.getCycle());
        }
    }
}