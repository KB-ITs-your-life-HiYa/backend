package com.fledge.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_survey_tag")
@IdClass(MemberSurveyTagId.class)
public class MemberSurveyTag {

    @Id
    private Long memberId;

    @Id
    private String tag;

    protected MemberSurveyTag() {}

    public MemberSurveyTag(Long memberId, String tag) {
        this.memberId = memberId;
        this.tag = tag;
    }

    public Long getMemberId() { return memberId; }
    public String getTag() { return tag; }
}