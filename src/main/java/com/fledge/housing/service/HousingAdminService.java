package com.fledge.housing.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.housing.collect.HousingCollector;
import com.fledge.member.domain.MemberRole;
import com.fledge.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HousingAdminService {
    private final MemberRepository memberRepository;
    private final HousingCollector collector;

    public HousingCollector.CollectResult collect(Long memberId) {
        // 토큰 발급 이후 역할이 변경돼도 DB의 현재 권한으로 판단한다.
        var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (member.getRole() != MemberRole.COUNSELOR) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return collector.collect();
    }
}
