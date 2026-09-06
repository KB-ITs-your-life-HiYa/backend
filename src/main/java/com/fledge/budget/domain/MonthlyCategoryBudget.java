package com.fledge.budget.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "monthly_category_budget")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyCategoryBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_budget_id", nullable = false)
    private MonthlyBudget monthlyBudget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public MonthlyCategoryBudget(ExpenseCategory category, Long amount) {
        this.category = category;
        this.amount = amount;
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void attach(MonthlyBudget monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    void updateAmount(Long amount) {
        this.amount = amount;
        this.updatedAt = OffsetDateTime.now();
    }
}