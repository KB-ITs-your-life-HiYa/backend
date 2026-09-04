package com.fledge.budget.dto;

import com.fledge.budget.domain.Account;
import com.fledge.budget.domain.AccountType;

import java.util.List;

public record AccountListResponse(
        AccountType accountType,
        long totalBalance,
        List<AccountItem> accounts
) {
    public static AccountListResponse from(AccountType type, List<Account> accounts) {
        List<AccountItem> items = accounts.stream().map(AccountItem::from).toList();
        long total = accounts.stream().mapToLong(Account::getBalance).sum();
        return new AccountListResponse(type, total, items);
    }

    public record AccountItem(String bankName, AccountType accountType, long balance) {
        public static AccountItem from(Account account) {
            return new AccountItem(account.getBankName(), account.getAccountType(), account.getBalance());
        }
    }
}