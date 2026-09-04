package com.fledge.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "member_survey")
public class MemberSurvey {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "household_size")
    private Integer householdSize;

    @Column(name = "income_pct_bracket")
    private Integer incomePctBracket;

    @Column(name = "is_benefit_recipient")
    private Boolean isBenefitRecipient;

    @Column(name = "employment_status")
    private String employmentStatus;

    @Column(name = "housing_type")
    private String housingType;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected MemberSurvey() {}

    public MemberSurvey(Long memberId) {
        this.memberId = memberId;
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getMemberId() { return memberId; }
    public Integer getHouseholdSize() { return householdSize; }
    public Integer getIncomePctBracket() { return incomePctBracket; }
    public Boolean getIsBenefitRecipient() { return isBenefitRecipient; }
    public String getEmploymentStatus() { return employmentStatus; }
    public String getHousingType() { return housingType; }

    public void setHouseholdSize(Integer v) { this.householdSize = v; }
    public void setIncomePctBracket(Integer v) { this.incomePctBracket = v; }
    public void setIsBenefitRecipient(Boolean v) { this.isBenefitRecipient = v; }
    public void setEmploymentStatus(String v) { this.employmentStatus = v; }
    public void setHousingType(String v) { this.housingType = v; }
    public void setUpdatedAt(java.time.OffsetDateTime v) { this.updatedAt = v; }
}