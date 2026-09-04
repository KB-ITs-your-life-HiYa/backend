package com.fledge.care.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "referral_request")
@Getter
@Setter
public class ReferralRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private Long careSignalId;
    private Long counselorId;
    private String status;
    private String reason;
    private Integer riskScoreAtRequest;
    private OffsetDateTime requestedAt;
}
