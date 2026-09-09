package com.fledge.budget.dto;

import com.fledge.budget.domain.ExpenseCategory;

import java.util.List;

public record ExpenseReportResponse(
        String month,
        Summary summary,
        MonthlyTrend monthlyTrend,
        List<CategoryBreakdown> categories,
        Navigation navigation,
        Long monthlyBudget,
        Coaching coaching
) {
    public record Summary(long totalExpense, long totalIncome) {
    }

    public record MonthlyTrend(List<MonthPoint> months, long averageExpense) {
        public record MonthPoint(String month, long totalExpense) {
        }
    }

    public record CategoryBreakdown(
            ExpenseCategory category,
            long currentAmount,
            long previousAmount,
            long difference,
            int progressRatio,
            Long budget
    ) {
    }

    public record Navigation(boolean hasPrevious, boolean hasNext) {
    }

    // 평소(직전 몇 달 평균) 대비 저번 달 지출 증감률 기반 코칭 문구. 증감률이 -10~10% 사이(평소와
    // 비슷한 수준)이거나 판정 불가(비교할 평균을 낼 이전 달 데이터가 없음)면 coaching 자체가 null —
    // 프론트는 null이면 문구를 그리지 않는다.
    // 완성 문장 대신 판정 결과와 숫자만 내려서, 프론트가 금액에 색을 입히거나 문구를 자유롭게 다듬을 수 있게 한다.
    public record Coaching(
            Tier tier,
            int changeRate,
            Long savedAmount,
            Long excessAmount,
            List<SurgeCategory> surgeCategories,
            Long reductionTargetAmount
    ) {
        public enum Tier { SURPLUS, CAUTION, DEFICIT }

        public record SurgeCategory(ExpenseCategory category, long increaseAmount) {
        }
    }
}