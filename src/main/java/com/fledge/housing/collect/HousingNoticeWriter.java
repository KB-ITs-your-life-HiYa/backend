package com.fledge.housing.collect;

import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.HousingNoticeUnit;
import com.fledge.housing.repository.HousingNoticeRepository;
import com.fledge.housing.repository.HousingNoticeUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 수집한 행을 DB 에 쓴다.
 *
 * 【수집기와 클래스를 나눈 이유】
 * @Transactional 은 스프링이 만든 프록시를 거쳐야 걸린다.
 * 같은 클래스 안에서 호출하면 프록시를 타지 않아 트랜잭션이 없는 채로 실행된다.
 * 페이지 단위로 트랜잭션을 끊으려면 다른 빈이어야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HousingNoticeWriter {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CORRECTION_NOTICE = "정정공고";

    private final HousingNoticeRepository noticeRepository;
    private final HousingNoticeUnitRepository unitRepository;

    /**
     * 한 페이지를 저장한다. 이 메서드 하나가 트랜잭션 하나다.
     * 전체를 한 트랜잭션으로 묶으면 한 페이지가 실패할 때 전부 롤백된다.
     */
    @Transactional
    public Saved savePage(List<MyHomeItem> items) {
        // API 는 "공고 × 단지" 를 펼친 행을 준다. 같은 공고가 단지 수만큼 반복된다.
        // 순서를 유지해야 단지 순서가 API 와 같아진다.
        Map<String, List<MyHomeItem>> byNotice = new LinkedHashMap<>();
        for (MyHomeItem item : items) {
            if (isBlank(item.pblancId())) {
                continue;
            }
            byNotice.computeIfAbsent(item.pblancId(), k -> new ArrayList<>()).add(item);
        }

        int units = 0;
        for (Map.Entry<String, List<MyHomeItem>> entry : byNotice.entrySet()) {
            HousingNotice notice = upsertNotice(entry.getKey(), entry.getValue().get(0));
            units += replaceUnits(notice.getId(), entry.getValue());
        }
        return new Saved(byNotice.size(), units);
    }

    /**
     * 정정공고가 대체한 원래 공고를 캘린더에서 뺀다.
     * 삭제하지 않는 이유는 그 공고를 관심 등록한 회원이 있을 수 있어서다.
     */
    @Transactional
    public int markSuperseded() {
        List<String> replaced = noticeRepository.findAll().stream()
                .filter(n -> CORRECTION_NOTICE.equals(n.getSttusNm()))
                .map(HousingNotice::getBeforePblancId)
                .filter(id -> !isBlank(id))
                .distinct()
                .toList();

        int marked = 0;
        for (String beforeId : replaced) {
            HousingNotice before = noticeRepository.findByPblancId(beforeId).orElse(null);
            if (before != null && !before.isSuperseded()) {
                before.markSuperseded();
                marked++;
            }
        }
        return marked;
    }

    private HousingNotice upsertNotice(String pblancId, MyHomeItem first) {
        // 값을 채운 뒤에 저장한다.
        // 빈 엔티티를 먼저 save 하면 NOT NULL 인 pblanc_nm 이 비어 있어 INSERT 가 실패한다.
        HousingNotice notice = noticeRepository.findByPblancId(pblancId)
                .orElseGet(() -> new HousingNotice(pblancId));

        notice.update(
                first.pblancNm(),
                nullIfBlank(first.suplyInsttNm()),
                nullIfBlank(first.houseTyNm()),
                nullIfBlank(first.suplyTyNm()),
                TargetTypeResolver.resolve(first.pblancNm()),
                nullIfBlank(first.sttusNm()),
                nullIfBlank(first.beforePblancId()),
                toDate(first.rcritPblancDe()),
                toDate(first.beginDe()),
                toDate(first.endDe()),
                toDate(first.przwnerPresnatnDe()),
                nullIfBlank(first.refrnc()),
                nullIfBlank(first.url()),
                nullIfBlank(first.pcUrl()));

        // 새 공고면 INSERT 되고 id 가 채워진다. 기존 공고면 이미 영속 상태라 변경 감지로 갱신된다.
        return noticeRepository.save(notice);
    }

    /** 공고별로 단지를 전부 지우고 다시 넣는다. 부분 갱신보다 단순하고 단지 수가 적어 비용도 낮다 */
    private int replaceUnits(Long noticeId, List<MyHomeItem> rows) {
        unitRepository.deleteByNoticeId(noticeId);

        List<HousingNoticeUnit> units = rows.stream()
                .map(r -> new HousingNoticeUnit(
                        noticeId,
                        toInt(r.houseSn()),
                        nullIfBlank(r.hsmpNm()),
                        nullIfBlank(r.brtcNm()),
                        nullIfBlank(r.signguNm()),
                        nullIfBlank(r.fullAdres()),
                        nullIfBlank(r.heatMthdNm()),
                        toInt(r.totHshldCo()),
                        toInt(r.sumSuplyCo()),
                        toLong(r.rentGtn()),
                        toLong(r.mtRntchrg()),
                        toLong(r.enty()),
                        toLong(r.surlus())))
                .toList();

        unitRepository.saveAll(units);
        return units.size();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    /** 같은 필드에 숫자와 빈 문자열이 섞여 오므로, 바꿀 수 없으면 null 로 둔다 */
    private static Integer toInt(String s) {
        Long v = toLong(s);
        return v == null ? null : v.intValue();
    }

    private static Long toLong(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            log.debug("숫자로 바꿀 수 없는 값: {}", s);
            return null;
        }
    }

    private static LocalDate toDate(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim(), YYYYMMDD);
        } catch (Exception e) {
            log.debug("날짜로 바꿀 수 없는 값: {}", s);
            return null;
        }
    }

    public record Saved(int notices, int units) {
    }
}
