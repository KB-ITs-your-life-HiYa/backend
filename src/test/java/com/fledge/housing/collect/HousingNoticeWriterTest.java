package com.fledge.housing.collect;

import com.fledge.housing.domain.HousingNotice;
import com.fledge.housing.domain.TargetType;
import com.fledge.housing.repository.HousingNoticeRepository;
import com.fledge.housing.repository.HousingNoticeUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 저장 로직만 검증한다. 외부 API 는 호출하지 않는다 --
 * CI 에는 서비스 키가 없고, 공공 API 응답에 테스트가 좌우되면 안 된다.
 */
@SpringBootTest
class HousingNoticeWriterTest {

    @Autowired HousingNoticeWriter writer;
    @Autowired HousingNoticeRepository noticeRepository;
    @Autowired HousingNoticeUnitRepository unitRepository;

    @BeforeEach
    void clean() {
        unitRepository.deleteAll();
        noticeRepository.deleteAll();
    }

    /** 실제 API 응답을 본뜬 행. 공고 하나에 단지 두 개 */
    private MyHomeItem row(String pblancId, String pblancNm, String hsmpNm, String totHshldCo) {
        return new MyHomeItem(
                pblancId, pblancNm, "LH", "아파트", "행복주택", "일반공고", "",
                "20260903", "20260928", "20260929", "20261007",
                "LH 콜센터 : 1600-1004", "https://apply.lh.or.kr", "https://www.myhome.go.kr",
                "2", hsmpNm, "대구광역시", "동구", "대구광역시 동구 메디밸리로 18 ", "지역난방",
                totHshldCo, "171", "15390000", "71820", "500000", "14890000");
    }

    @Test
    void 같은_공고의_여러_행을_공고1건과_단지N건으로_나눈다() {
        HousingNoticeWriter.Saved saved = writer.savePage(List.of(
                row("21152", "대구혁신10 행복주택 입주자 모집", "대구혁신10", "1088"),
                row("21152", "대구혁신10 행복주택 입주자 모집", "경산하양3", "512")));

        assertThat(saved.notices()).isEqualTo(1);
        assertThat(saved.units()).isEqualTo(2);
        assertThat(noticeRepository.findAll()).hasSize(1);
        assertThat(unitRepository.findAll()).hasSize(2);
    }

    @Test
    void 값을_제대로_옮긴다() {
        writer.savePage(List.of(row("21152", "2026년 청년 전세임대 수시모집", "대구혁신10", "1088")));

        HousingNotice notice = noticeRepository.findByPblancId("21152").orElseThrow();
        assertThat(notice.getPblancNm()).isEqualTo("2026년 청년 전세임대 수시모집");
        assertThat(notice.getTargetType()).isEqualTo(TargetType.YOUTH);
        assertThat(notice.getBeginDe()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(notice.getEndDe()).isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(notice.getCollectedAt()).isNotNull();
        // 빈 문자열은 null 로 둔다. "" 를 그대로 넣으면 화면에서 빈칸이 보인다
        assertThat(notice.getBeforePblancId()).isNull();

        var unit = unitRepository.findByNoticeId(notice.getId()).get(0);
        assertThat(unit.getRentGtn()).isEqualTo(15_390_000L);
        assertThat(unit.getMtRntchrg()).isEqualTo(71_820L);
        assertThat(unit.getBrtcNm()).isEqualTo("대구광역시");
    }

    @Test
    void 세대수가_빈_문자열이면_null_이다() {
        // 실측에서 totHshldCo 는 319행 중 65행이 "" 였다.
        // Integer 로 받으면 역직렬화에서 터지므로 String 으로 받아 파싱한다
        writer.savePage(List.of(row("21152", "행복주택 모집", "대구혁신10", "")));

        HousingNotice notice = noticeRepository.findByPblancId("21152").orElseThrow();
        assertThat(unitRepository.findByNoticeId(notice.getId()).get(0).getTotHshldCo()).isNull();
    }

    @Test
    void 다시_수집해도_늘어나지_않는다() {
        List<MyHomeItem> page = List.of(
                row("21152", "행복주택 모집", "대구혁신10", "1088"),
                row("21152", "행복주택 모집", "경산하양3", "512"));

        writer.savePage(page);
        writer.savePage(page);

        assertThat(noticeRepository.findAll()).hasSize(1);
        // 단지는 공고별로 지우고 다시 넣으므로 중복이 쌓이지 않는다
        assertThat(unitRepository.findAll()).hasSize(2);
    }

    @Test
    void 공고_내용이_바뀌면_갱신된다() {
        writer.savePage(List.of(row("21152", "행복주택 모집", "대구혁신10", "1088")));
        writer.savePage(List.of(row("21152", "행복주택 모집 (정정)", "대구혁신10", "1088")));

        assertThat(noticeRepository.findByPblancId("21152").orElseThrow().getPblancNm())
                .isEqualTo("행복주택 모집 (정정)");
    }

    @Test
    void 정정공고가_대체한_공고를_캘린더에서_뺀다() {
        writer.savePage(List.of(row("21152", "행복주택 모집", "대구혁신10", "1088")));

        MyHomeItem base = row("21999", "행복주택 모집 정정", "대구혁신10", "1088");
        MyHomeItem correction = new MyHomeItem(
                base.pblancId(), base.pblancNm(), base.suplyInsttNm(), base.houseTyNm(),
                base.suplyTyNm(), "정정공고", "21152",
                base.rcritPblancDe(), base.beginDe(), base.endDe(), base.przwnerPresnatnDe(),
                base.refrnc(), base.url(), base.pcUrl(),
                base.houseSn(), base.hsmpNm(), base.brtcNm(), base.signguNm(),
                base.fullAdres(), base.heatMthdNm(), base.totHshldCo(), base.sumSuplyCo(),
                base.rentGtn(), base.mtRntchrg(), base.enty(), base.surlus());

        writer.savePage(List.of(correction));
        assertThat(writer.markSuperseded()).isEqualTo(1);

        // 삭제하지 않는다. 그 공고를 관심 등록한 회원이 있을 수 있다
        assertThat(noticeRepository.findByPblancId("21152").orElseThrow().isSuperseded()).isTrue();
        assertThat(noticeRepository.findByPblancId("21999").orElseThrow().isSuperseded()).isFalse();
    }
}
