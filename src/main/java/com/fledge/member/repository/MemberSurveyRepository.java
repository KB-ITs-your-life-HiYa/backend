package com.fledge.member.repository;

import com.fledge.member.domain.MemberSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSurveyRepository extends JpaRepository<MemberSurvey, Long> {
}