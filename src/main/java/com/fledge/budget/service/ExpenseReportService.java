package com.fledge.budget.service;

import com.fledge.budget.domain.ExpenseCategory;
import com.fledge.budget.domain.FinancialTransaction;
import com.fledge.budget.domain.MonthlyBudget;
import com.fledge.budget.dto.ExpenseReportResponse;
import com.fledge.budget.dto.ExpenseReportResponse.CategoryBreakdown;
import com.fledge.budget.dto.ExpenseReportResponse.Coaching;
import com.fledge.budget.dto.ExpenseReportResponse.Coaching.SurgeCategory;
import com.fledge.budget.dto.ExpenseReportResponse.Coaching.Tier;
import com.fledge.budget.dto.ExpenseReportResponse.MonthlyTrend;
import com.fledge.budget.dto.ExpenseReportResponse.MonthlyTrend.MonthPoint;
import com.fledge.budget.dto.ExpenseReportResponse.Navigation;
import com.fledge.budget.dto.ExpenseReportResponse.Summary;
import com.fledge.budget.dto.ExpenseSummaryResponse;
import com.fledge.budget.repository.FinancialTransactionRepository;
import com.fledge.budget.repository.MonthlyBudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseReportService {
    private final FinancialTransactionRepository transactionRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;

    public ExpenseSummaryResponse getSummary(Long memberId) {
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthSameDay = today.minusMonths(1);
        LocalDate lastMonthStart = currentMonthStart.minusMonths(1);

        List<FinancialTransaction> txns = transactionRepository
                .findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(memberId, lastMonthStart, today);

        long currentTotal = TransactionAggregator.sumExpense(txns, currentMonthStart, today);
        long lastPeriodTotal = TransactionAggregator.sumExpense(txns, lastMonthStart, lastMonthSameDay);
        return ExpenseSummaryResponse.of(currentTotal, lastPeriodTotal);
    }

    public ExpenseReportResponse getReport(Long memberId, YearMonth month) {
        LocalDate today = LocalDate.now();
        boolean isCurrentMonth = month.equals(YearMonth.now());
        YearMonth prevMonth = month.minusMonths(1);

        // 코칭용 평균은 prevMonth 이전 3개월(prevMonth-1~3)까지 봐야 해서 month-4까지 넉넉히 가져온다.
        LocalDate fetchFrom = month.minusMonths(4).atDay(1);
        LocalDate fetchTo = month.atEndOfMonth();
        List<FinancialTransaction> txns = transactionRepository
                .findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(memberId, fetchFrom, fetchTo);

        // 진행 중인 달이면 오늘까지만, 이미 끝난 달이면 월 전체.
        // 시드 등으로 미래 날짜 거래가 있을 수 있어 "미래 거래가 없다"는 가정에 기대지 않고 명시적으로 자른다.
        LocalDate currentPeriodEnd = isCurrentMonth ? today : month.atEndOfMonth();

        // a) 해당 월 요약
        long totalExpense = TransactionAggregator.sumExpense(txns, month.atDay(1), currentPeriodEnd);
        long totalIncome = TransactionAggregator.sumIncome(txns, month.atDay(1), currentPeriodEnd);

        // b) 최근 3개월 그래프 — 조회 중인 달 포인트는 위와 같은 기준(currentPeriodEnd)을 쓴다.
        List<MonthPoint> monthPoints = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            YearMonth m = month.minusMonths(i);
            LocalDate pointEnd = m.equals(month) ? currentPeriodEnd : m.atEndOfMonth();
            monthPoints.add(new MonthPoint(m.toString(), TransactionAggregator.sumExpense(txns, m.atDay(1), pointEnd)));
        }
        // 월 평균지출 — 조회 중인 달(month)은 아직 다 지나지 않았을 수 있어 평균에서 제외하고,
        // 나머지 달 중에서도 지출 데이터가 있는(totalExpense > 0) 달만으로 평균을 낸다.
        long averageExpense = Math.round(
                monthPoints.stream()
                        .filter(mp -> !mp.month().equals(month.toString()))
                        .filter(mp -> mp.totalExpense() > 0)
                        .mapToLong(MonthPoint::totalExpense)
                        .average()
                        .orElse(0));

        // c) 카테고리별 — 이번 달(진행 중이면 1일~오늘)을 지난달 "전체"와 비교한다.
        //    지난달 쪽을 같은 기간으로 잘라서 보여주면, 그 달이 지나고 나서 다시 보면
        //    (이제는 "지난달"이 아니라 "이번 달"이 되어) 월 전체 합계로 바뀌어 같은 달인데 값이 달라 보인다.
        //    그래서 지난달은 항상 월 전체로 고정한다. (expense-summary 의 "지난달 같은 기간" 비교와는 별개 — 그쪽은 그대로 둔다)
        LocalDate prevPeriodEnd = prevMonth.atEndOfMonth();

        Map<ExpenseCategory, Long> currentByCategory = new EnumMap<>(ExpenseCategory.class);
        Map<ExpenseCategory, Long> previousByCategory = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory c : ExpenseCategory.values()) {
            currentByCategory.put(c, TransactionAggregator.sumCategory(txns, c, month.atDay(1), currentPeriodEnd));
            previousByCategory.put(c, TransactionAggregator.sumCategory(txns, c, prevMonth.atDay(1), prevPeriodEnd));
        }
        long maxCurrent = currentByCategory.values().stream().mapToLong(Long::longValue).max().orElse(0);

        // 예산 — 있으면 상단 요약·그래프 점선·카테고리 기준으로 쓰인다. 없으면 프론트가 월 평균/지난달로 대체한다.
        MonthlyBudget budget = monthlyBudgetRepository.findByMemberIdAndBudgetMonth(memberId, month.atDay(1)).orElse(null);

        List<CategoryBreakdown> categories = new ArrayList<>();
        for (ExpenseCategory c : ExpenseCategory.values()) {
            long cur = currentByCategory.get(c);
            long prev = previousByCategory.get(c);
            int ratio = maxCurrent == 0 ? 0 : (int) Math.round(cur * 100.0 / maxCurrent);
            Long categoryBudget = budget == null ? null : budget.amountOf(c).orElse(null);
            categories.add(new CategoryBreakdown(c, cur, prev, cur - prev, ratio, categoryBudget));
        }

        // d) 월 이동 가능 여부 — 이전 달은 이미 가져온 데이터 안에 포함되어 있어 필터링만
        boolean hasPrevious = txns.stream()
                .anyMatch(t -> !t.getTxnDate().isBefore(prevMonth.atDay(1)) && !t.getTxnDate().isAfter(prevMonth.atEndOfMonth()));
        YearMonth nextMonth = month.plusMonths(1);
        boolean hasNext = !transactionRepository
                .findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(memberId, nextMonth.atDay(1), nextMonth.atEndOfMonth())
                .isEmpty();

        // e) 지출 코칭 — 조회 중인 달(month)이 아니라 그 전달(prevMonth)의 지출을 판정한다.
        //    급증 카테고리는 prevMonth 대 prevMonth의 전달(month-2) 증가액으로 잡는데, 마침 이미
        //    fetch해둔 범위(month-4 ~ month)에 그대로 들어있어 추가 쿼리가 필요 없다.
        long prevTotalExpense = TransactionAggregator.sumExpense(txns, prevMonth.atDay(1), prevPeriodEnd);
        Coaching coaching = buildCoaching(txns, prevMonth, prevTotalExpense);

        return new ExpenseReportResponse(
                month.toString(),
                new Summary(totalExpense, totalIncome),
                new MonthlyTrend(monthPoints, averageExpense),
                categories,
                new Navigation(hasPrevious, hasNext),
                budget == null ? null : budget.getTotalAmount(),
                coaching
        );
    }

    // 저번 달 지출이 "평소"(prevMonth 이전 3개월 평균) 대비 얼마나 늘거나 줄었는지로 코칭 문구
    // 판정 자료를 만든다. 자립준비청년은 수입이 자립수당 등으로 고정·소액인 경우가 많아 지출률
    // (지출/수입)로는 구간이 갈리지 않아서, 자기 과거 지출과 비교하는 방식으로 바꿨다.
    //   증감률 = (저번 달 지출 - 평균) / 평균 * 100
    //   -10% 이하   : 저축여력 (SURPLUS)
    //   -10%~+10%  : 평소와 비슷 — 코칭 없음 (null)
    //   +10%~+20%  : 주의 (CAUTION) — 급증 카테고리 1개
    //   +20% 초과   : 적자 (DEFICIT) — 급증 카테고리 2개
    // 평균은 prevMonth 이전 3개월 중 지출이 실제로 있었던(>0) 달만으로 낸다(prevMonth 자신은
    // 제외 — 포함하면 편차가 줄어든다). 그런 달이 하나도 없으면 판정 불가로 코칭을 내리지 않는다.
    private Coaching buildCoaching(List<FinancialTransaction> txns, YearMonth prevMonth, long prevTotalExpense) {
        List<Long> baselineMonthTotals = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            YearMonth m = prevMonth.minusMonths(i);
            long total = TransactionAggregator.sumExpense(txns, m.atDay(1), m.atEndOfMonth());
            if (total > 0) baselineMonthTotals.add(total);
        }
        if (baselineMonthTotals.isEmpty()) return null;
        double average = baselineMonthTotals.stream().mapToLong(Long::longValue).average().orElseThrow();

        double rate = (prevTotalExpense - average) / average * 100;
        Tier tier;
        if (rate <= -10) tier = Tier.SURPLUS;
        else if (rate <= 10) return null;
        else if (rate <= 20) tier = Tier.CAUTION;
        else tier = Tier.DEFICIT;

        int changeRate = (int) Math.round(rate);
        Long savedAmount = tier == Tier.SURPLUS ? Math.round(average - prevTotalExpense) : null;
        Long excessAmount = tier == Tier.DEFICIT ? Math.round(prevTotalExpense - average) : null;

        List<SurgeCategory> surgeCategories = List.of();
        Long reductionTargetAmount = null;
        if (tier == Tier.CAUTION || tier == Tier.DEFICIT) {
            YearMonth beforePrevMonth = prevMonth.minusMonths(1);
            int limit = tier == Tier.CAUTION ? 1 : 2;
            surgeCategories = Arrays.stream(ExpenseCategory.values())
                    .filter(c -> c != ExpenseCategory.SAVINGS) // 저축 이체 증가는 절감 대상이 아니다
                    .map(c -> new SurgeCategory(c,
                            TransactionAggregator.sumCategory(txns, c, prevMonth.atDay(1), prevMonth.atEndOfMonth())
                                    - TransactionAggregator.sumCategory(txns, c, beforePrevMonth.atDay(1), beforePrevMonth.atEndOfMonth())))
                    .filter(s -> s.increaseAmount() > 0)
                    .sorted(Comparator.comparingLong(SurgeCategory::increaseAmount).reversed())
                    .limit(limit)
                    .toList();
            long increaseSum = surgeCategories.stream().mapToLong(SurgeCategory::increaseAmount).sum();
            reductionTargetAmount = increaseSum == 0 ? null : Math.round(increaseSum / 2.0);
        }

        return new Coaching(tier, changeRate, savedAmount, excessAmount, surgeCategories, reductionTargetAmount);
    }
}