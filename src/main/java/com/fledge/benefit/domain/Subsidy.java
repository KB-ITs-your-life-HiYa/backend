package com.fledge.benefit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "subsidy")
public class Subsidy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rawId;

    private String source;
    private String externalId;

    private String name;
    private String summary;
    private String orgName;

    private String categoryRaw;
    private String category;

    private String targetRaw;
    private String benefitRaw;

    private String applyMethod;
    private String applyDeadlineRaw;
    private LocalDate applyDeadlineDate;

    private LocalDate bizStartDate;
    private LocalDate bizEndDate;

    private String detailUrl;

    private Integer minAge;
    private Integer maxAge;

    private Integer incomePctMax;
    private Long incomeAmtMin;
    private Long incomeAmtMax;

    private String protectionStatusRequired;
    private BigDecimal minYearsAfterEnd;
    private BigDecimal maxYearsAfterEnd;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> targetHousehold;

    private String exclusionGroup;
    private Long duplicateOfSubsidyId;

    private boolean needsManualReview = true;   // DB DEFAULT true 와 맞춰서 자바 쪽도 기본값 true

    private LocalDateTime createdAt;

    protected Subsidy() {}

    //꼭 필요한 4개(source, externalId, rawId, name)만 생성자로 받고 나머지는 setter로 채우는 방식
    public Subsidy(String source, String externalId, Long rawId, String name) {
        this.source = source;
        this.externalId = externalId;
        this.rawId = rawId;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public void setSummary(String summary) { this.summary = summary; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public void setCategory(String category) { this.category = category; }
    public void setTargetRaw(String targetRaw) { this.targetRaw = targetRaw; }
    public void setBenefitRaw(String benefitRaw) { this.benefitRaw = benefitRaw; }
    public void setApplyMethod(String applyMethod) { this.applyMethod = applyMethod; }
    public void setApplyDeadlineRaw(String applyDeadlineRaw) { this.applyDeadlineRaw = applyDeadlineRaw; }
    public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
    public void setIncomePctMax(Integer incomePctMax) { this.incomePctMax = incomePctMax; }
    public void setIncomeAmtMin(Long incomeAmtMin) { this.incomeAmtMin = incomeAmtMin; }
    public void setIncomeAmtMax(Long incomeAmtMax) { this.incomeAmtMax = incomeAmtMax; }
    public void setProtectionStatusRequired(String v) { this.protectionStatusRequired = v; }
    public void setMinYearsAfterEnd(BigDecimal v) { this.minYearsAfterEnd = v; }
    public void setMaxYearsAfterEnd(BigDecimal v) { this.maxYearsAfterEnd = v; }
    public void setTargetHousehold(List<String> v) { this.targetHousehold = v; }
}