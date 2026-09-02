package com.fledge.benefit.ingest;

import java.math.BigDecimal;
import java.util.List;

public record ParsedSubsidy(
        Long rawId,
        String name,
        String summary,
        String orgName,
        String category,
        String applyMethod,
        String applyDeadlineRaw,
        String detailUrl,
        Integer minAge,
        Integer maxAge,
        Integer incomePctMax,
        Long incomeAmtMin,
        Long incomeAmtMax,
        String protectionStatusRequired,
        BigDecimal minYearsAfterEnd,
        BigDecimal maxYearsAfterEnd,
        List<String> targetHousehold,
        List<ParsedBenefit> benefits,
        List<ParsedRegion> regions
) {
    public record ParsedBenefit(String benefitName, Long amountKrw, String cycle) {}
    public record ParsedRegion(String sidoCode, String sigunguCode) {}
}