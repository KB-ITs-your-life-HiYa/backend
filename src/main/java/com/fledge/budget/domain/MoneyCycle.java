package com.fledge.budget.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "money_cycle")
@Getter
@Setter
public class MoneyCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private Long scheduleId;
    private java.time.LocalDate cycleMonth;
    private java.time.LocalDate expectedDate;
    private Long expectedAmount;
    private String status;
    private Long matchedTransactionId;
    private java.time.LocalDate actualDate;
    private Long actualAmount;
    private java.time.OffsetDateTime reminderSentAt;
    private java.time.OffsetDateTime updatedAt;
}
