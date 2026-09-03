package com.fledge.budget.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "bank_name")
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    private Long balance;

    @Column(name = "balance_updated_at")
    private OffsetDateTime balanceUpdatedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}