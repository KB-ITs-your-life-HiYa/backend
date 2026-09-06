package com.fledge.budget.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Entity
@Table(name = "monthly_budget")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "budget_month", nullable = false)
    private LocalDate budgetMonth;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "monthlyBudget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthlyCategoryBudget> categoryBudgets = new ArrayList<>();

    public MonthlyBudget(Long memberId, LocalDate budgetMonth, Long totalAmount) {
        this.memberId = memberId;
        this.budgetMonth = budgetMonth;
        this.totalAmount = totalAmount;
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
        this.updatedAt = OffsetDateTime.now();
    }

    // 전달받은 (카테고리 -> 금액) 으로 통째로 갈아끼운다.
    // 그대로 남는 카테고리는 금액만 갱신하고, 빠진 카테고리만 지우고 새 카테고리만 추가한다.
    //   category.id 가 IDENTITY 생성이라, 같은 카테고리를 지우고 새로 만드는 방식(clear 후 재생성)을 쓰면
    //   Hibernate 가 flush 시 orphanRemoval 삭제보다 새 insert 를 먼저 실행해 유니크 제약을 위반한다.
    //   그래서 남는 카테고리는 반드시 기존 엔티티를 그대로 두고 금액만 바꿔야 한다.
    public void replaceCategoryBudgets(Map<ExpenseCategory, Long> amounts) {
        categoryBudgets.removeIf(b -> !amounts.containsKey(b.getCategory()));
        for (MonthlyCategoryBudget b : categoryBudgets) {
            b.updateAmount(amounts.get(b.getCategory()));
        }
        for (Map.Entry<ExpenseCategory, Long> e : amounts.entrySet()) {
            if (amountOf(e.getKey()).isEmpty()) {
                MonthlyCategoryBudget budget = new MonthlyCategoryBudget(e.getKey(), e.getValue());
                categoryBudgets.add(budget);
                budget.attach(this);
            }
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public Optional<Long> amountOf(ExpenseCategory category) {
        return categoryBudgets.stream()
                .filter(b -> b.getCategory() == category)
                .map(MonthlyCategoryBudget::getAmount)
                .findFirst();
    }
}