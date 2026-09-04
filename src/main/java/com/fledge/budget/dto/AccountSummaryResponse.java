package com.fledge.budget.dto;

import com.fledge.budget.domain.Account;
import com.fledge.budget.domain.AccountType;

import java.util.List;

public record AccountSummaryResponse(
        long depositTotal,
        long savingsTotal,
        long netAsset
) {
    public static AccountSummaryResponse from(List<Account> accounts) {
        long depositTotal = sumOf(accounts, AccountType.DEPOSIT);
        long savingsTotal = sumOf(accounts, AccountType.SAVINGS);
        return new AccountSummaryResponse(depositTotal, savingsTotal, depositTotal + savingsTotal);
    }

    private static long sumOf(List<Account> accounts, AccountType type) {
        return accounts.stream()
                .filter(a -> a.getAccountType() == type)
                .mapToLong(Account::getBalance)
                .sum();
    }
}