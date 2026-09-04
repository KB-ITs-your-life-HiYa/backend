package com.fledge.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    // 자립준비청년 가준 - 보호종료 후 5년 (자립수당 60개월과 같은 기간)
    private static final int SELF_RELIANCE_DAYS = 1825;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "region_sigungu")
    private String regionSigungu;

    @Enumerated(EnumType.STRING)
    @Column(name = "protection_status")
    private ProtectionStatus protectionStatus;

    @Column(name = "protection_end_date")
    private LocalDate protectionEndDate;

    @Column(name = "protection_expected_end")
    private LocalDate protectionExpectedEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "protection_type")
    private ProtectionType protectionType;

    // 보호종료 당시 어디 (자립정착금 산정 기준에 활용)
    @Column(name = "home_region_code")
    private String homeRegionCode;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    // D-1825까지 남은 일수. 보호중이면 null. 음수면 이미 지났음을 의미
    public Long getDaysUntilSupportEnd() {
        if(protectionEndDate == null) return null;
        return ChronoUnit.DAYS.between(
                LocalDate.now(),protectionEndDate.plusDays(SELF_RELIANCE_DAYS));
    }

    public EligibilityTier getEligibilityTier() {
        Long left = getDaysUntilSupportEnd();
        if(left != null && left >= 0) return EligibilityTier.SELF_RELIANCE;
        if(getAge() <= 39) return EligibilityTier.YOUTH;
        return EligibilityTier.GENERAL;
    }
}
