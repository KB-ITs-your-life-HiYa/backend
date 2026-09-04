package com.fledge.member.repository;

import com.fledge.member.domain.MemberSurveyTag;
import com.fledge.member.domain.MemberSurveyTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberSurveyTagRepository extends JpaRepository<MemberSurveyTag, MemberSurveyTagId> {
    List<MemberSurveyTag> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}