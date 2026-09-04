package com.fledge.care.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "care_signal")
@Getter
@Setter
public class CareSignal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private Long moneyCycleId;
    private String signalType;
    private String status;
    private String responseResult;
    private String classificationSource;
    private java.time.OffsetDateTime detectedAt;
    private java.time.OffsetDateTime recheckAt;
    private java.time.OffsetDateTime recheckedAt;
    private java.time.OffsetDateTime resolvedAt;
    private java.time.OffsetDateTime updatedAt;
}
