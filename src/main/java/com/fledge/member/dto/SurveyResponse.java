package com.fledge.member.dto;

import com.fledge.member.domain.MemberSurvey;

import java.util.List;

public record SurveyResponse(
        Integer householdSize,
        Integer incomePctBracket,
        Boolean isBenefitRecipient,
        String employmentStatus,
        String housingType,
        List<String> tags
) {
    public static SurveyResponse of(MemberSurvey survey, List<String> tags) {
        return new SurveyResponse(
                survey.getHouseholdSize(),
                survey.getIncomePctBracket(),
                survey.getIsBenefitRecipient(),
                survey.getEmploymentStatus(),
                survey.getHousingType(),
                tags
        );
    }
}