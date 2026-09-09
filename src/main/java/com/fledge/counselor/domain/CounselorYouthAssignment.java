package com.fledge.counselor.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "counselor_youth_assignment")
@Getter
@Setter
public class CounselorYouthAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long counselorId;
    private Long youthMemberId;
    private OffsetDateTime assignedAt;
    private OffsetDateTime unassignedAt;
}
