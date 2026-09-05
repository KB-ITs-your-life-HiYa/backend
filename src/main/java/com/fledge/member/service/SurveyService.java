package com.fledge.member.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.member.domain.MemberSurvey;
import com.fledge.member.domain.MemberSurveyTag;
import com.fledge.member.dto.SurveyRequest;
import com.fledge.member.dto.SurveyResponse;
import com.fledge.member.repository.MemberSurveyRepository;
import com.fledge.member.repository.MemberSurveyTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private static final Set<Integer> VALID_INCOME_BRACKETS =
            Set.of(32, 48, 50, 60, 100, 120, 150, 999);
    private static final Set<String> VALID_TAGS = Set.of(
            "SINGLE_PARENT", "MULTICULTURAL", "DISABILITY",
            "MULTI_CHILD", "SEVERE_ILLNESS", "NORTH_KOREAN_DEFECTOR", "GRANDPARENT_FAMILY"
    );

    private final MemberSurveyRepository memberSurveyRepository;
    private final MemberSurveyTagRepository memberSurveyTagRepository;

    /** 저장된 설문이 없으면 null. 프론트에서 "설문을 아직 안 한 회원"을 구분하는 용도 */
    public SurveyResponse findMe(Long memberId) {
        return memberSurveyRepository.findById(memberId)
                .map(survey -> SurveyResponse.of(survey, tagsOf(memberId)))
                .orElse(null);
    }

    @Transactional
    public SurveyResponse save(Long memberId, SurveyRequest request) {
        if (request.incomePctBracket() != null
                && !VALID_INCOME_BRACKETS.contains(request.incomePctBracket())) {
            throw new ApiException(ErrorCode.MEMBER_SURVEY_INVALID, "소득 구간 값이 올바르지 않습니다");
        }

        List<String> tags = request.tags() == null ? List.of() : request.tags();
        for (String tag : tags) {
            if (!VALID_TAGS.contains(tag)) {
                throw new ApiException(ErrorCode.MEMBER_SURVEY_INVALID, "알 수 없는 태그입니다: " + tag);
            }
        }

        MemberSurvey survey = memberSurveyRepository.findById(memberId)
                .orElseGet(() -> new MemberSurvey(memberId));
        survey.setHouseholdSize(request.householdSize());
        survey.setIncomePctBracket(request.incomePctBracket());
        survey.setIsBenefitRecipient(request.isBenefitRecipient());
        survey.setEmploymentStatus(request.employmentStatus());
        survey.setHousingType(request.housingType());
        survey.setUpdatedAt(OffsetDateTime.now());
        memberSurveyRepository.save(survey);

        memberSurveyTagRepository.deleteByMemberId(memberId);
        List<MemberSurveyTag> tagEntities = tags.stream()
                .map(tag -> new MemberSurveyTag(memberId, tag))
                .toList();
        memberSurveyTagRepository.saveAll(tagEntities);

        return SurveyResponse.of(survey, tags);
    }

    private List<String> tagsOf(Long memberId) {
        return memberSurveyTagRepository.findByMemberId(memberId).stream()
                .map(MemberSurveyTag::getTag)
                .toList();
    }
}