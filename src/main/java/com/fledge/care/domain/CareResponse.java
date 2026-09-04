package com.fledge.care.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "care_response")
@Getter
@Setter
public class CareResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long careSignalId;
    private String inputType;
    private String selectedValue;
    private String inputText;
    private String aiReply;
    private String ruleReply;
    private String requestId;
    private String requestPayload;
    private String policyStatus;
    @Column(columnDefinition = "text")
    private String policyCards;
    private java.time.OffsetDateTime createdAt;
}
