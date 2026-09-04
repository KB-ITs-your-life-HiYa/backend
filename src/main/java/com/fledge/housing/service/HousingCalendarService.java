package com.fledge.housing.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.housing.dto.HousingCalendarResponse;
import com.fledge.housing.dto.HousingNoticeSummary;
import com.fledge.housing.repository.HousingNoticeRepository;
import com.fledge.housing.repository.HousingNoticeUnitRepository;
import com.fledge.member.repository.MemberRepository;
import com.fledge.region.repository.SidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HousingCalendarService {

    /** 전국 보기로 바꾸고 싶을 때 프론트가 보내는 값 */
    private static final String NATIONWIDE = "ALL";

    /** 지역 공고가 이 개수 미만이면 전국으로 자동 전환한다. 실측 기준 서울이 5건이라 이 값 밑에서 실제로 걸린다 */
    private static final int MIN_NOTICES_BEFORE_FALLBACK = 6;

    private static final Comparator<HousingNoticeSummary> CALENDAR_ORDER =
            Comparator.comparingInt((HousingNoticeSummary s) -> s.targetType().ordinal())
                    .thenComparing(HousingNoticeSummary::beginDate);

    private final HousingNoticeRepository noticeRepository;
    private final HousingNoticeUnitRepository unitRepository;
    private final MemberRepository memberRepository;
    private final SidoRepository sidoRepository;

    public HousingCalendarResponse findByMonth(int year, int month, Long memberId, String regionCode) {
        YearMonth target = YearMonth.of(year, month);
        LocalDate monthStart = target.atDay(1);
        LocalDate monthEnd = target.atEndOfMonth();

        List<HousingNoticeSummary> all = findAllInMonth(monthStart, monthEnd);

        // "ALL" 을 명시적으로 요청하면 필터 없이 전국을 보여준다
        if (NATIONWIDE.equals(regionCode)) {
            return toResponse(all, monthStart, monthEnd, null, null);
        }

        // 파라미터가 없으면 로그인한 회원의 거주 시/도를 기본값으로 쓴다
        String effectiveCode = (regionCode != null) ? regionCode : memberRegionCode(memberId);
        if (effectiveCode == null) {
            return toResponse(all, monthStart, monthEnd, null, null);
        }

        String regionName = sidoRepository.findById(effectiveCode)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "존재하지 않는 지역 코드입니다"))
                .getName();

        Set<Long> idsInRegion = new HashSet<>(unitRepository.findDistinctNoticeIdByRegionName(regionName));
        List<HousingNoticeSummary> filtered = all.stream()
                .filter(n -> idsInRegion.contains(n.id()))
                .toList();

        // 그 지역 공고가 너무 적으면 빈 화면 대신 전국으로 보여주고 이유를 알린다
        if (filtered.size() < MIN_NOTICES_BEFORE_FALLBACK) {
            return toResponse(all, monthStart, monthEnd, null,
                    regionName + " 지역 공고가 적어 전국 공고를 보여드려요");
        }

        return toResponse(filtered, monthStart, monthEnd, effectiveCode, null);
    }

    private List<HousingNoticeSummary> findAllInMonth(LocalDate monthStart, LocalDate monthEnd) {
        return noticeRepository
                .findBySupersededFalseAndBeginDeLessThanEqualAndEndDeGreaterThanEqual(monthEnd, monthStart)
                .stream()
                .map(HousingNoticeSummary::from)
                .sorted(CALENDAR_ORDER)
                .toList();
    }

    /**
     * 캘린더에 점을 찍을 수 있는 것과, 이번 달에 시작도 마감도 없는 상시 모집을 나눈다.
     *
     * 판정 기준(설계 문서): 조회 중인 달에 점이 하나도 안 찍히면서, 그 달 내내 접수 중인 공고.
     * 시작일·마감일 둘 다 이번 달 범위 밖이면 상시 모집이다.
     */
    private HousingCalendarResponse toResponse(List<HousingNoticeSummary> list,
                                                LocalDate monthStart, LocalDate monthEnd,
                                                String appliedRegionCode, String message) {
        List<HousingNoticeSummary> notices = new ArrayList<>();
        List<HousingNoticeSummary> ongoingNotices = new ArrayList<>();

        for (HousingNoticeSummary n : list) {
            boolean hasDotThisMonth = isWithin(n.beginDate(), monthStart, monthEnd)
                    || isWithin(n.endDate(), monthStart, monthEnd);
            (hasDotThisMonth ? notices : ongoingNotices).add(n);
        }

        return new HousingCalendarResponse(notices, ongoingNotices, appliedRegionCode, message);
    }

    private static boolean isWithin(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private String memberRegionCode(Long memberId) {
        return memberRepository.findById(memberId)
                .map(m -> m.getRegionCode())
                .orElse(null);
    }
}
