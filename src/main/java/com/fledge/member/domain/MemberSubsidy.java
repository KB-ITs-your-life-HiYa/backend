package com.fledge.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "member_subsidy")
@IdClass(MemberSubsidyId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSubsidy {

    @Id
    private Long memberId;

    @Id
    private Long subsidyId;

    private OffsetDateTime createdAt;

    public MemberSubsidy(Long memberId, Long subsidyId) {
        this.memberId = memberId;
        this.subsidyId = subsidyId;
        this.createdAt = OffsetDateTime.now();
    }
}
