package com.fledge.budget.service;

import com.fledge.budget.domain.Account;
import com.fledge.budget.domain.AccountType;
import com.fledge.budget.dto.AccountListResponse;
import com.fledge.budget.dto.AccountSummaryResponse;
import com.fledge.budget.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountSummaryResponse getSummary(Long memberId) {
        return AccountSummaryResponse.from(accountRepository.findByMemberId(memberId));
    }

    public AccountListResponse getAccounts(Long memberId, AccountType type) {
        List<Account> accounts = accountRepository.findByMemberId(memberId).stream()
                .filter(a -> a.getAccountType() == type)
                .toList();
        return AccountListResponse.from(type, accounts);
    }
}