package com.fledge.budget.service;

import com.fledge.budget.domain.ExpenseCategory;
import com.fledge.budget.domain.FinancialTransaction;
import com.fledge.budget.dto.AccountSummaryResponse;
import com.fledge.budget.dto.SupportEndForecastResponse;
import com.fledge.budget.dto.SupportEndForecastResponse.CategoryReduction;
import com.fledge.budget.dto.SupportEndForecastResponse.Forecast;
import com.fledge.budget.dto.SupportEndForecastResponse.Reduction;
import com.fledge.budget.repository.AccountRepository;
import com.fledge.budget.repository.FinancialTransactionRepository;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.member.domain.Member;
import com.fledge.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportEndForecastService {

    // 자립수당 월 지급액. 정책 금액이 바뀌면 여기만 고친다.
    private static final long ALLOWANCE_MONTHLY_AMOUNT = 500_000L;

    // 노출 조건: 수당 종료까지 이 일수 이내
    private static final long ELIGIBLE_WITHIN_DAYS = 365;

    // 평균에 쓸 완결된 달 개수 (진행 중인 이번 달은 제외)
    private static final int MAX_AVERAGE_MONTHS = 3;

    // 10% 절감 시뮬레이션 대상. 고정비(HOUSING_UTILITY)와 SAVINGS는 줄일 대상이 아니라 제외
    private static final List<ExpenseCategory> REDUCIBLE_CATEGORIES = List.of(
            ExpenseCategory.FOOD, ExpenseCategory.LEISURE_SHOPPING,
            ExpenseCategory.LIVING_MEDICAL, ExpenseCategory.TRANSPORT
    );

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;

    public SupportEndForecastResponse getForecast(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        Long daysUntilSupportEnd = member.getDaysUntilSupportEnd();
        if (daysUntilSupportEnd == null || daysUntilSupportEnd < 0 || daysUntilSupportEnd > ELIGIBLE_WITHIN_DAYS) {
            return SupportEndForecastResponse.notEligible();
        }

        long monthsUntilSupportEnd = daysUntilSupportEnd / 30;
        long currentSavingsTotal =
                AccountSummaryResponse.from(accountRepository.findByMemberId(memberId)).savingsTotal();

        // 완결된 지난 달만 후보로 삼는다 (진행 중인 이번 달을 넣으면 월초에 평균이 왜곡됨)
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> candidateMonths = new ArrayList<>();
        for (int i = 1; i <= MAX_AVERAGE_MONTHS; i++) {
            candidateMonths.add(currentMonth.minusMonths(i));
        }
        YearMonth earliestCandidate = candidateMonths.get(candidateMonths.size() - 1);

        List<FinancialTransaction> txns = transactionRepository.findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(
                memberId, earliestCandidate.atDay(1), currentMonth.minusMonths(1).atEndOfMonth());

        // 후보 중 실제 거래가 있는 달만 평균에 쓴다 (데이터 없는 달을 0으로 넣으면 평균이 저평가됨)
        List<YearMonth> monthsWithData = candidateMonths.stream()
                .filter(m -> txns.stream().anyMatch(t -> TransactionAggregator.inRange(t, m.atDay(1), m.atEndOfMonth())))
                .toList();

        if (monthsWithData.isEmpty()) {
            Forecast forecast = new Forecast(
                    daysUntilSupportEnd, monthsUntilSupportEnd, 0, false,
                    null, null, null, currentSavingsTotal, null, null
            );
            return new SupportEndForecastResponse(true, forecast);
        }

        int monthsUsed = monthsWithData.size();
        long totalExpense = 0;
        long totalIncome = 0;
        for (YearMonth m : monthsWithData) {
            totalExpense += TransactionAggregator.sumExpense(txns, m.atDay(1), m.atEndOfMonth());
            totalIncome += TransactionAggregator.sumIncome(txns, m.atDay(1), m.atEndOfMonth());
        }
        long averageExpense = totalExpense / monthsUsed;
        long averageIncome = totalIncome / monthsUsed;

        long incomeExcludingAllowance = Math.max(0, averageIncome - ALLOWANCE_MONTHLY_AMOUNT);
        long monthlyShortfall = Math.max(0, averageExpense - incomeExcludingAllowance);

        Long savingsRunwayMonths = monthlyShortfall == 0 ? null : currentSavingsTotal / monthlyShortfall;

        List<CategoryReduction> categoryReductions = new ArrayList<>();
        long totalMonthlySavings = 0;
        for (ExpenseCategory category : REDUCIBLE_CATEGORIES) {
            long categoryTotal = 0;
            for (YearMonth m : monthsWithData) {
                categoryTotal += TransactionAggregator.sumCategory(txns, category, m.atDay(1), m.atEndOfMonth());
            }
            long categoryAverage = categoryTotal / monthsUsed;
            long reducedAmount = Math.round(categoryAverage * 0.9);
            long monthlySavings = categoryAverage - reducedAmount;
            totalMonthlySavings += monthlySavings;
            categoryReductions.add(new CategoryReduction(category, categoryAverage, reducedAmount, monthlySavings));
        }

        long totalSavingsByEnd = totalMonthlySavings * monthsUntilSupportEnd;
        Long improvedRunwayMonths = monthlyShortfall == 0
                ? null
                : (currentSavingsTotal + totalSavingsByEnd) / monthlyShortfall;

        Reduction reduction = new Reduction(categoryReductions, totalMonthlySavings, totalSavingsByEnd, improvedRunwayMonths);
        Forecast forecast = new Forecast(
                daysUntilSupportEnd, monthsUntilSupportEnd, monthsUsed, true,
                incomeExcludingAllowance, averageExpense, monthlyShortfall, currentSavingsTotal, savingsRunwayMonths, reduction
        );
        return new SupportEndForecastResponse(true, forecast);
    }
}