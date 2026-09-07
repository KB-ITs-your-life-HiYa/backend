package com.fledge.member.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.member.domain.Member;
import com.fledge.member.dto.MemberResponse;
import com.fledge.member.repository.MemberRepository;
import com.fledge.region.service.RegionNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final RegionNameResolver regionNameResolver;

    public MemberResponse findMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        String regionName = regionNameResolver.resolve(member.getRegionCode(), member.getRegionSigunguCode());
        return MemberResponse.from(member, regionName);
    }
}
