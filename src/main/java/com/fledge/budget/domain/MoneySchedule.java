package com.fledge.budget.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "money_schedule")
@Getter
@Setter
public class MoneySchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    private String direction;
    private String type;
    private String name;
    private Long expectedAmount;
    private Integer expectedDay;
    private String matchKeyword;
    private boolean isActive;
    private java.time.OffsetDateTime updatedAt;
}
