package com.fledge.budget.repository;

import com.fledge.budget.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByMemberId(Long memberId);
}