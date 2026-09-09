package com.fledge.counselor.service;

import com.fledge.budget.domain.MoneyCycle;
import com.fledge.budget.domain.MoneySchedule;
import com.fledge.budget.repository.MoneyCycleRepository;
import com.fledge.budget.repository.MoneyScheduleRepository;
import com.fledge.care.domain.CareResponse;
import com.fledge.care.domain.CareSignal;
import com.fledge.care.domain.ReferralRequest;
import com.fledge.care.repository.CareResponseRepository;
import com.fledge.care.repository.CareSignalRepository;
import com.fledge.care.repository.ReferralRequestRepository;
import com.fledge.common.ErrorCode;
import com.fledge.counselor.domain.Counselor;
import com.fledge.counselor.dto.CounselorReferralResponse;
import com.fledge.counselor.repository.CounselorRepository;
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
public class CounselorReferralService {
    private final MemberRepository memberRepository;
    private final CounselorRepository counselorRepository;
    private final ReferralRequestRepository referralRepository;
    private final CareSignalRepository signalRepository;
    private final CareResponseRepository responseRepository;
    private final MoneyCycleRepository cycleRepository;
    private final MoneyScheduleRepository scheduleRepository;
    private final RegionNameResolver regionNameResolver;

    public List<CounselorReferralResponse> findMine(Long loginMemberId) {
        Member loginMember = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (loginMember.getRole() != MemberRole.COUNSELOR) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        Counselor counselor = counselorRepository.findByMemberIdAndActiveTrue(loginMemberId)
                .orElseThrow(() -> new ApiException(ErrorCode.COUNSELOR_PROFILE_NOT_FOUND));

        return referralRepository.findByCounselorIdOrderByRequestedAtDescIdDesc(counselor.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private CounselorReferralResponse toResponse(ReferralRequest referral) {
        Member youth = memberRepository.findById(referral.getMemberId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        CareSignal signal = signalRepository.findByIdAndMemberId(referral.getCareSignalId(), youth.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.CARE_SIGNAL_NOT_FOUND));
        MoneyCycle cycle = cycleRepository.findByIdAndMemberId(signal.getMoneyCycleId(), youth.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        MoneySchedule schedule = scheduleRepository.findByIdAndMemberId(cycle.getScheduleId(), youth.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        CareResponse latestResponse = responseRepository
                .findFirstByCareSignalIdOrderByCreatedAtDescIdDesc(signal.getId())
                .orElse(null);

        int riskScore = referral.getRiskScoreAtRequest() == null ? 0 : referral.getRiskScoreAtRequest();
        return new CounselorReferralResponse(
                referral.getId(),
                youth.getId(),
                youth.getName() == null ? youth.getEmail() : youth.getName(),
                youth.getPhone(),
                youth.getAge(),
                regionNameResolver.resolve(youth.getRegionCode(), youth.getRegionSigunguCode()),
                youth.getProtectionStatus(),
                referral.getStatus(),
                riskScore,
                riskLevel(riskScore),
                signal.getSignalType(),
                situation(signal.getSignalType(), schedule.getName()),
                latestUserMessage(latestResponse, signal.getSignalType()),
                referral.getRequestedAt(),
                referral.getContactedAt(),
                referral.getClosedAt());
    }

    private String riskLevel(int riskScore) {
        if (riskScore == 0) return "NORMAL";
        return riskScore < 60 ? "CARE" : "HUMAN_CARE";
    }

    private String situation(String signalType, String scheduleName) {
        return switch (signalType) {
            case "MISSED_SAVING" -> scheduleName + " 적금 납입이 확인되지 않았습니다";
            case "MISSED_PAYMENT" -> scheduleName + " 납부가 확인되지 않았습니다";
            default -> scheduleName + " 입금이 확인되지 않았습니다";
        };
    }

    private String latestUserMessage(CareResponse response, String signalType) {
        if (response == null) return null;
        if (response.getInputText() != null && !response.getInputText().isBlank()) {
            return response.getInputText();
        }
        if (response.getSelectedValue() == null) return null;
        return switch (response.getSelectedValue()) {
            case "DIFFICULT" -> switch (signalType) {
                case "MISSED_SAVING" -> "현재 적금 납입이 어렵다고 응답했습니다";
                case "MISSED_PAYMENT" -> "현재 납부가 어렵다고 응답했습니다";
                default -> "현재 소득 상황이 어렵다고 응답했습니다";
            };
            case "LATER" -> "상담 응답을 나중으로 미뤘습니다";
            case "ALREADY_DONE" -> "이미 처리했다고 응답했습니다";
            case "CHANGED" -> "기존 일정을 변경했다고 응답했습니다";
            default -> null;
        };
    }
}
