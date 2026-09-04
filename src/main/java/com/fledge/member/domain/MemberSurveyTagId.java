package com.fledge.member.domain;

import java.io.Serializable;
import java.util.Objects;

public class MemberSurveyTagId implements Serializable {
    private Long memberId;
    private String tag;

    public MemberSurveyTagId() {}

    public MemberSurveyTagId(Long memberId, String tag) {
        this.memberId = memberId;
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberSurveyTagId that)) return false;
        return Objects.equals(memberId, that.memberId) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, tag);
    }
}