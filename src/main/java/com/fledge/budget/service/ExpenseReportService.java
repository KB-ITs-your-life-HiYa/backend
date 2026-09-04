package com.fledge.budget.service;

import com.fledge.budget.domain.ExpenseCategory;
import com.fledge.budget.domain.FinancialTransaction;
import com.fledge.budget.dto.ExpenseReportResponse;
import com.fledge.budget.dto.ExpenseReportResponse.CategoryBreakdown;
import com.fledge.budget.dto.ExpenseReportResponse.MonthlyTrend;
import com.fledge.budget.dto.ExpenseReportResponse.MonthlyTrend.MonthPoint;
import com.fledge.budget.dto.ExpenseReportResponse.Navigation;
import com.fledge.budget.dto.ExpenseReportResponse.Summary;
import com.fledge.budget.dto.ExpenseSummaryResponse;
import com.fledge.budget.repository.FinancialTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseReportService {
    private final FinancialTransactionRepository transactionRepository;

    public ExpenseSummaryResponse getSummary(Long memberId) {
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthSameDay = today.minusMonths(1);
        LocalDate lastMonthStart = currentMonthStart.minusMonths(1);

        List<FinancialTransaction> txns = transactionRepository
                .findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(memberId, lastMonthStart, today);

        long currentTotal = sumExpense(txns, currentMonthStart, today);
        long lastPeriodTotal = sumExpense(txns, lastMonthStart, lastMonthSameDay);
        return ExpenseSummaryResponse.of(currentTotal, lastPeriodTotal);
    }

    public ExpenseReportResponse getReport(Long memberId, YearMonth month) {
        LocalDate today = LocalDate.now();
        boolean isCurrentMonth = month.equals(YearMonth.now());
        YearMonth prevMonth = month.minusMonths(1);

        LocalDate fetchFrom = month.minusMonths(2).atDay(1);
        LocalDate fetchTo = month.atEndOfMonth();
        List<FinancialTransaction> txns = transactionRepository
                .findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(memberId, fetchFrom, fetchTo);

        // a) 해당 월 요약 — 항상 월 전체 범위 (진행 중인 달이어도 미래 거래가 없어 자연히 오늘까지만 잡힘)
        long totalExpense = sumExpense(txns, month.atDay(1), month.atEndOfMonth());
        long totalIncome = sumIncome(txns, month.atDay(1), month.atEndOfMonth());

        // b) 최근 3개월 그래프
        List<MonthPoint> monthPoints = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            YearMonth m = month.minusMonths(i);
            monthPoints.add(new MonthPoint(m.toString(), sumExpense(txns, m.atDay(1), m.atEndOfMonth())));
        }
        long averageExpense = Math.round(
                monthPoints.stream().mapToLong(MonthPoint::totalExpense).average().orElse(0));

        // c) 카테고리별 — 진행 중인 달이면 "1일~오늘" 같은 기간, 끝난 달이면 월 전체로 비교
        LocalDate currentPeriodEnd = isCurrentMonth ? today : month.atEndOfMonth();
        LocalDate prevPeriodEnd = isCurrentMonth ? today.minusMonths(1) : prevMonth.atEndOfMonth();

        Map<ExpenseCategory, Long> currentByCategory = new EnumMap<>(ExpenseCategory.class);
        Map<ExpenseCategory, Long> previousByCategory = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory c : ExpenseCategory.values()) {
            currentByCategory.put(c, sumCategory(txns, c, month.atDay(1), currentPeriodEnd));
            previousByCategory.put(c, sumCategory(txns, c, prevMonth.atDay(1), prevPeriodEnd));
        }
        long maxCurrent = currentByCategory.values().stream().mapToLong(Long::longValue).max().orElse(0);

        List<CategoryBreakdown> categories = new ArrayList<>();
        for (ExpenseCategory c : ExpenseCategory.values()) {
            long cur = currentByCategory.get(c);
            long prev = previousByCategory.get(c);
            int ratio = maxCurrent == 0 ? 0 : (int) Math.round(cur * 100.0 / maxCurrent);
            categories.add(new CategoryBreakdown(c, cur, prev, cur - prev, ratio, null));
        }

        // d) 월 이동 가능 여부 — 이전 달은 이미 가져온 데이터 안에 포함되어 있어 필터링만
        boolean hasPrevious = txns.stream()
                .anyMatch(t -> !t.getTxnDate().isBefore(prevMonth.atDay(1)) && !t.getTxnDate().isAfter(prevMonth.atEndOfMonth()));
        YearMonth nextMonth = month.plusMonths(1);
        boolean hasNext = !transactionRepository
                .findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(memberId, nextMonth.atDay(1), nextMonth.atEndOfMonth())
                .isEmpty();

        return new ExpenseReportResponse(
                month.toString(),
                new Summary(totalExpense, totalIncome),
                new MonthlyTrend(monthPoints, averageExpense),
                categories,
                new Navigation(hasPrevious, hasNext),
                null
        );
    }

    private long sumExpense(List<FinancialTransaction> txns, LocalDate from, LocalDate to) {
        return txns.stream()
                .filter(t -> "EXPENSE".equals(t.getTxnType()))
                .filter(t -> !"SAVINGS".equals(t.getCategory()))
                .filter(t -> inRange(t, from, to))
                .mapToLong(FinancialTransaction::getAmount)
                .sum();
    }

    private long sumIncome(List<FinancialTransaction> txns, LocalDate from, LocalDate to) {
        return txns.stream()
                .filter(t -> "INCOME".equals(t.getTxnType()))
                .filter(t -> inRange(t, from, to))
                .mapToLong(FinancialTransaction::getAmount)
                .sum();
    }

    private long sumCategory(List<FinancialTransaction> txns, ExpenseCategory category, LocalDate from, LocalDate to) {
        return txns.stream()
                .filter(t -> "EXPENSE".equals(t.getTxnType()))
                .filter(t -> category.name().equals(t.getCategory()))
                .filter(t -> inRange(t, from, to))
                .mapToLong(FinancialTransaction::getAmount)
                .sum();
    }

    private boolean inRange(FinancialTransaction t, LocalDate from, LocalDate to) {
        return !t.getTxnDate().isBefore(from) && !t.getTxnDate().isAfter(to);
    }
}