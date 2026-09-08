package com.fledge.member.service;

import com.fledge.benefit.domain.Subsidy;
import com.fledge.benefit.repository.SubsidyRepository;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.member.domain.MemberSubsidy;
import com.fledge.member.domain.MemberSubsidyId;
import com.fledge.benefit.dto.SubsidySummaryResponse;
import com.fledge.member.repository.MemberSubsidyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSubsidyService {

    private final MemberSubsidyRepository memberSubsidyRepository;
    private final SubsidyRepository subsidyRepository;

    public List<SubsidySummaryResponse> findMine(Long memberId) {
        List<Long> subsidyIds = memberSubsidyRepository.findByMemberId(memberId).stream()
                .map(MemberSubsidy::getSubsidyId)
                .toList();
        return subsidyRepository.findAllById(subsidyIds).stream()
                .map(SubsidySummaryResponse::from)
                .toList();
    }

    @Transactional
    public void add(Long memberId, Long subsidyId) {
        Subsidy subsidy = subsidyRepository.findById(subsidyId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "지원금을 찾을 수 없습니다"));
        MemberSubsidyId id = new MemberSubsidyId(memberId, subsidy.getId());
        if (memberSubsidyRepository.existsById(id)) return; // 이미 추가돼 있으면 조용히 무시
        memberSubsidyRepository.save(new MemberSubsidy(memberId, subsidy.getId()));
    }

    @Transactional
    public void remove(Long memberId, Long subsidyId) {
        memberSubsidyRepository.deleteByMemberIdAndSubsidyId(memberId, subsidyId);
    }
}
