package com.fledge.budget.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction")
@Getter
@Setter
public class FinancialTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private Long accountId;
    private java.time.LocalDate txnDate;
    private String txnType;
    private Long amount;
    private String merchantName;
    private String category;
}
