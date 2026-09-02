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
}