package com.fledge.member.repository;

import com.fledge.member.domain.MemberSubsidy;
import com.fledge.member.domain.MemberSubsidyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberSubsidyRepository extends JpaRepository<MemberSubsidy, MemberSubsidyId> {
    List<MemberSubsidy> findByMemberId(Long memberId);
    void deleteByMemberIdAndSubsidyId(Long memberId, Long subsidyId);
}
