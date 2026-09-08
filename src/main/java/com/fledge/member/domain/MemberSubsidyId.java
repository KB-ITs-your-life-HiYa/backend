package com.fledge.member.domain;

import java.io.Serializable;
import java.util.Objects;

public class MemberSubsidyId implements Serializable {
    private Long memberId;
    private Long subsidyId;

    public MemberSubsidyId() {}

    public MemberSubsidyId(Long memberId, Long subsidyId) {
        this.memberId = memberId;
        this.subsidyId = subsidyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberSubsidyId that)) return false;
        return Objects.equals(memberId, that.memberId) && Objects.equals(subsidyId, that.subsidyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, subsidyId);
    }
}
