package com.fledge.housing.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 임대주택 공고. 캘린더의 점 하나가 이 테이블 한 줄이다.
 *
 * 컬럼명은 마이홈포털 API 필드명을 그대로 따른다.
 * 원문과 대조하기 쉽고, 옮겨 적다 생기는 실수를 줄인다.
 */
@Getter
@Entity
@Table(name = "housing_notice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousingNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pblanc_id")
    private String pblancId;

    @Column(name = "pblanc_nm")
    private String pblancNm;

    @Column(name = "suply_instt_nm")
    private String suplyInsttNm;

    @Column(name = "house_ty_nm")
    private String houseTyNm;

    @Column(name = "suply_ty_nm")
    private String suplyTyNm;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType;

    @Column(name = "sttus_nm")
    private String sttusNm;

    @Column(name = "before_pblanc_id")
    private String beforePblancId;

    /** 정정공고에 대체됨. 캘린더에서 제외한다 */
    private boolean superseded;

    @Column(name = "rcrit_pblanc_de")
    private LocalDate rcritPblancDe;

    @Column(name = "begin_de")
    private LocalDate beginDe;

    @Column(name = "end_de")
    private LocalDate endDe;

    @Column(name = "przwner_presnatn_de")
    private LocalDate przwnerPresnatnDe;

    private String refrnc;
    private String url;

    @Column(name = "pc_url")
    private String pcUrl;

    @Column(name = "collected_at")
    private OffsetDateTime collectedAt;

    public HousingNotice(String pblancId) {
        this.pblancId = pblancId;
    }

    /**
     * 수집한 값으로 덮어쓴다.
     * 공고는 정정될 수 있으므로 매번 전체를 갱신한다.
     */
    public void update(String pblancNm, String suplyInsttNm, String houseTyNm, String suplyTyNm,
                       TargetType targetType, String sttusNm, String beforePblancId,
                       LocalDate rcritPblancDe, LocalDate beginDe, LocalDate endDe,
                       LocalDate przwnerPresnatnDe, String refrnc, String url, String pcUrl) {
        this.pblancNm = pblancNm;
        this.suplyInsttNm = suplyInsttNm;
        this.houseTyNm = houseTyNm;
        this.suplyTyNm = suplyTyNm;
        this.targetType = targetType;
        this.sttusNm = sttusNm;
        this.beforePblancId = beforePblancId;
        this.rcritPblancDe = rcritPblancDe;
        this.beginDe = beginDe;
        this.endDe = endDe;
        this.przwnerPresnatnDe = przwnerPresnatnDe;
        this.refrnc = refrnc;
        this.url = url;
        this.pcUrl = pcUrl;
        this.collectedAt = OffsetDateTime.now();
    }

    /** 정정공고에 대체되었음을 표시한다. 삭제하지 않는 이유는 관심 등록이 걸려 있을 수 있어서다 */
    public void markSuperseded() {
        this.superseded = true;
    }
}
