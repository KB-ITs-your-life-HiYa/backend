package com.fledge.counselor.service;

import com.fledge.common.ErrorCode;
import com.fledge.counselor.domain.Counselor;
import com.fledge.counselor.domain.CounselorYouthAssignment;
import com.fledge.counselor.dto.CounselorYouthResponse;
import com.fledge.counselor.repository.CounselorRepository;
import com.fledge.counselor.repository.CounselorYouthAssignmentRepository;
import com.fledge.exception.ApiException;
import com.fledge.member.domain.Member;
import com.fledge.member.domain.MemberRole;
import com.fledge.member.repository.MemberRepository;
import com.fledge.region.service.RegionNameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselorYouthService {
    private final MemberRepository memberRepository;
    private final CounselorRepository counselorRepository;
    private final CounselorYouthAssignmentRepository assignmentRepository;
    private final RegionNameResolver regionNameResolver;

    public List<CounselorYouthResponse> findMine(Long loginMemberId) {
        Member loginMember = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (loginMember.getRole() != MemberRole.COUNSELOR) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        Counselor counselor = counselorRepository.findByMemberIdAndActiveTrue(loginMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.COUNSELOR_PROFILE_NOT_FOUND));

        return assignmentRepository.findActiveByCounselorId(counselor.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private CounselorYouthResponse toResponse(CounselorYouthAssignment assignment) {
        Member youth = memberRepository.findById(assignment.getYouthMemberId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return new CounselorYouthResponse(
                youth.getId(),
                youth.getName() == null ? youth.getEmail() : youth.getName(),
                youth.getPhone(),
                youth.getAge(),
                youth.getGender(),
                regionNameResolver.resolve(youth.getRegionCode(), youth.getRegionSigunguCode()),
                youth.getProtectionStatus(),
                youth.getProtectionEndDate(),
                youth.getDaysUntilSupportEnd(),
                assignment.getAssignedAt());
    }
}
