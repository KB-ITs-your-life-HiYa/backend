package com.fledge.housing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "housing_eligibility_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousingEligibilityProfile {
    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "is_homeless", nullable = false)
    private boolean homeless;

    @Column(name = "is_married", nullable = false)
    private boolean married;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public HousingEligibilityProfile(Long memberId) {
        this.memberId = memberId;
    }

    public void update(boolean homeless, boolean married) {
        this.homeless = homeless;
        this.married = married;
        this.updatedAt = OffsetDateTime.now();
    }
}
