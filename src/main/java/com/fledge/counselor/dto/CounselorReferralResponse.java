package com.fledge.counselor.dto;

import com.fledge.member.domain.ProtectionStatus;

import java.time.OffsetDateTime;

public record CounselorReferralResponse(
        Long id,
        Long youthId,
        String youthName,
        String phone,
        int age,
        String regionName,
        ProtectionStatus protectionStatus,
        String status,
        int riskScore,
        String riskLevel,
        String signalType,
        String situation,
        String latestUserMessage,
        OffsetDateTime requestedAt,
        OffsetDateTime contactedAt,
        OffsetDateTime closedAt
) {
}
