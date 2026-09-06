package com.fledge.budget.service;

import com.fledge.budget.domain.ExpenseCategory;
import com.fledge.budget.domain.FinancialTransaction;
import com.fledge.budget.domain.MonthlyBudget;
import com.fledge.budget.dto.BudgetResponse;
import com.fledge.budget.dto.SaveBudgetRequest;
import com.fledge.budget.repository.FinancialTransactionRepository;
import com.fledge.budget.repository.MonthlyBudgetRepository;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final MonthlyBudgetRepository budgetRepository;
    private final FinancialTransactionRepository transactionRepository;

    public BudgetResponse get(Long memberId, YearMonth month) {
        MonthlyBudget budget = budgetRepository.findByMemberIdAndBudgetMonth(memberId, month.atDay(1)).orElse(null);
        return BudgetResponse.of(month, budget, lastMonthAmounts(memberId, month));
    }

    @Transactional
    public BudgetResponse save(Long memberId, YearMonth month, SaveBudgetRequest request) {
        Map<ExpenseCategory, Long> categoryAmounts = new EnumMap<>(ExpenseCategory.class);
        for (SaveBudgetRequest.CategoryAmount c : request.categories()) {
            if (c.amount() < 0) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "카테고리 예산은 0 이상이어야 합니다");
            }
            if (c.amount() == 0) {
                continue; // 0 = 미설정 취급, 행을 만들지 않는다
            }
            categoryAmounts.put(c.category(), c.amount());
        }

        long categorySumExcludingSavings = categoryAmounts.entrySet().stream()
                .filter(e -> e.getKey() != ExpenseCategory.SAVINGS)
                .mapToLong(Map.Entry::getValue)
                .sum();
        if (categorySumExcludingSavings > request.totalAmount()) {
            throw new ApiException(ErrorCode.BUDGET_CATEGORY_SUM_EXCEEDS_TOTAL);
        }

        MonthlyBudget existing = budgetRepository.findByMemberIdAndBudgetMonth(memberId, month.atDay(1)).orElse(null);
        MonthlyBudget budget = existing != null ? existing : new MonthlyBudget(memberId, month.atDay(1), request.totalAmount());
        budget.updateTotalAmount(request.totalAmount());

        budget.replaceCategoryBudgets(categoryAmounts);

        // 이미 영속 상태인 엔티티를 다시 save() 하면 Hibernate 가 merge() 로 처리하면서
        // cascade 가 새 카테고리 행을 orphanRemoval 삭제보다 먼저 즉시 insert 해버려
        // 같은 카테고리가 유니크 제약에 걸린다. 새로 만든 경우에만 명시적으로 저장한다.
        if (existing == null) {
            budgetRepository.save(budget);
        }

        return BudgetResponse.of(month, budget, lastMonthAmounts(memberId, month));
    }

    @Transactional
    public void delete(Long memberId, YearMonth month) {
        MonthlyBudget budget = budgetRepository.findByMemberIdAndBudgetMonth(memberId, month.atDay(1))
                .orElseThrow(() -> new ApiException(ErrorCode.BUDGET_NOT_FOUND));
        budgetRepository.delete(budget);
    }

    // month 의 전달(前달) 카테고리별 사용액. 예산 설정 화면의 "지난달 ○○원" 참고값.
    private Map<ExpenseCategory, Long> lastMonthAmounts(Long memberId, YearMonth month) {
        YearMonth prevMonth = month.minusMonths(1);
        List<FinancialTransaction> txns = transactionRepository
                .findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(memberId, prevMonth.atDay(1), prevMonth.atEndOfMonth());

        Map<ExpenseCategory, Long> result = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory c : ExpenseCategory.values()) {
            result.put(c, TransactionAggregator.sumCategory(txns, c, prevMonth.atDay(1), prevMonth.atEndOfMonth()));
        }
        return result;
    }
}