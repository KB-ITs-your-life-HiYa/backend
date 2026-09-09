package com.fledge.counselor.dto;

import com.fledge.member.domain.Gender;
import com.fledge.member.domain.ProtectionStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CounselorYouthResponse(
        Long id,
        String name,
        String phone,
        int age,
        Gender gender,
        String regionName,
        ProtectionStatus protectionStatus,
        LocalDate protectionEndDate,
        Long daysUntilSupportEnd,
        OffsetDateTime assignedAt
) {
}
