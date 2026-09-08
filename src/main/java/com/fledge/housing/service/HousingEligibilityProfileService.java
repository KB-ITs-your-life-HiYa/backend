package com.fledge.housing.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.housing.domain.HousingEligibilityProfile;
import com.fledge.housing.dto.HousingEligibilityProfileRequest;
import com.fledge.housing.dto.HousingEligibilityProfileResponse;
import com.fledge.housing.repository.HousingEligibilityProfileRepository;
import com.fledge.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousingEligibilityProfileService {
    private final HousingEligibilityProfileRepository profileRepository;
    private final MemberRepository memberRepository;

    public HousingEligibilityProfileResponse findMe(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return profileRepository.findById(memberId)
                .map(HousingEligibilityProfileResponse::from)
                .orElse(null);
    }

    @Transactional
    public HousingEligibilityProfileResponse save(Long memberId, HousingEligibilityProfileRequest request) {
        memberRepository.lockForHousingEligibility(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        HousingEligibilityProfile profile = profileRepository.findById(memberId)
                .orElseGet(() -> new HousingEligibilityProfile(memberId));
        profile.update(request.isHomeless(), request.isMarried());
        return HousingEligibilityProfileResponse.from(profileRepository.save(profile));
    }
}
