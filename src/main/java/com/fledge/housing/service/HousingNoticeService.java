package com.fledge.housing.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.dto.HousingNoticeDetailResponse;
import com.fledge.housing.dto.HousingNoticeUnitResponse;
import com.fledge.housing.repository.HousingNoticeRepository;
import com.fledge.housing.repository.HousingNoticeUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousingNoticeService {

    private final HousingNoticeRepository noticeRepository;
    private final HousingNoticeUnitRepository unitRepository;

    public HousingNoticeDetailResponse findDetail(Long noticeId) {
        HousingNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ApiException(ErrorCode.HOUSING_NOTICE_NOT_FOUND));

        List<HousingNoticeUnitResponse> units = unitRepository.findByNoticeId(noticeId).stream()
                .sorted(Comparator.comparing(u -> u.getHouseSn() == null ? Integer.MAX_VALUE : u.getHouseSn()))
                .map(HousingNoticeUnitResponse::from)
                .toList();

        return HousingNoticeDetailResponse.from(notice, units);
    }
}
