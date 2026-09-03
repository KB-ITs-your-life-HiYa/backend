package com.fledge.housing.service;

import com.fledge.housing.dto.HousingNoticeSummary;
import com.fledge.housing.repository.HousingNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousingCalendarService {

    // 자립준비청년 -> 청년 -> 그 외 순으로 우선순위 정렬 (같은 우선순위 안에서는 접수 시작일이 빠른 것부터 보여주기)
    private static final Comparator<HousingNoticeSummary> CALENDAR_ORDER =
            Comparator.comparingInt((HousingNoticeSummary s) -> s.targetType().ordinal())
                    .thenComparing(HousingNoticeSummary::beginDate);

    private final HousingNoticeRepository noticeRepository;

    public List<HousingNoticeSummary> findByMonth(int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        LocalDate monthStart = target.atDay(1);
        LocalDate monthEnd = target.atEndOfMonth();

        return noticeRepository
                .findBySupersededFalseAndBeginDeLessThanEqualAndEndDeGreaterThanEqual(monthEnd, monthStart)
                .stream()
                .map(HousingNoticeSummary::from)
                .sorted(CALENDAR_ORDER)
                .toList();
    }
}
