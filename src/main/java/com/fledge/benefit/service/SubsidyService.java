package com.fledge.benefit.service;

import com.fledge.benefit.dto.SubsidySummaryResponse;
import com.fledge.benefit.repository.SubsidyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubsidyService {

    private static final int SEARCH_LIMIT = 20;

    private final SubsidyRepository subsidyRepository;

    // "받고 있는 지원금 추가" 화면에서 이름으로 검색. 우리 카탈로그 안에서만 찾는다
    public List<SubsidySummaryResponse> search(String query) {
        return subsidyRepository.findByNameContainingIgnoreCase(query, PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(SubsidySummaryResponse::from)
                .toList();
    }
}
