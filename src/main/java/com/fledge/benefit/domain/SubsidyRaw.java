package com.fledge.benefit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "subsidy_raw")
public class SubsidyRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    private String externalId;   // DB의 external_id 컬럼과 자동 매핑됨 (camelCase → snake_case)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawPayload;

    private LocalDateTime fetchedAt;

    protected SubsidyRaw() {}  // JPA가 요구하는 기본 생성자
}