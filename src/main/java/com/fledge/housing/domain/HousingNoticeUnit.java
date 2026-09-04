package com.fledge.housing.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공고에 딸린 단지·지역. 보증금·월세는 단지마다 다르다.
 *
 * 공고 하나에 단지가 평균 3개다. API 가 319행을 주는데 실제 공고는 100건이다.
 *
 * houseSn 은 유니크하지 않아(같은 공고 안에서 중복) 자연키로 쓸 수 없다.
 * 서로게이트 PK 가 필요하다.
 */
@Getter
@Entity
@Table(name = "housing_notice_unit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousingNoticeUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관관계 대신 원시 값으로 둔다.
     * 수집이 "공고별로 전부 지우고 다시 넣는" 방식이라 양방향 매핑이 필요 없고,
     * 지연 로딩으로 생기는 문제도 없다.
     */
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "house_sn")
    private Integer houseSn;

    @Column(name = "hsmp_nm")
    private String hsmpNm;

    /** 시도 이름. 지역 필터 기준 */
    @Column(name = "brtc_nm")
    private String brtcNm;

    @Column(name = "signgu_nm")
    private String signguNm;

    @Column(name = "full_adres")
    private String fullAdres;

    @Column(name = "heat_mthd_nm")
    private String heatMthdNm;

    @Column(name = "tot_hshld_co")
    private Integer totHshldCo;

    @Column(name = "sum_suply_co")
    private Integer sumSuplyCo;

    /** 보증금 */
    @Column(name = "rent_gtn")
    private Long rentGtn;

    /** 월세 */
    @Column(name = "mt_rntchrg")
    private Long mtRntchrg;

    /** 계약금 */
    private Long enty;

    /** 잔금 */
    private Long surlus;

    public HousingNoticeUnit(Long noticeId, Integer houseSn, String hsmpNm, String brtcNm,
                             String signguNm, String fullAdres, String heatMthdNm,
                             Integer totHshldCo, Integer sumSuplyCo,
                             Long rentGtn, Long mtRntchrg, Long enty, Long surlus) {
        this.noticeId = noticeId;
        this.houseSn = houseSn;
        this.hsmpNm = hsmpNm;
        this.brtcNm = brtcNm;
        this.signguNm = signguNm;
        this.fullAdres = fullAdres;
        this.heatMthdNm = heatMthdNm;
        this.totHshldCo = totHshldCo;
        this.sumSuplyCo = sumSuplyCo;
        this.rentGtn = rentGtn;
        this.mtRntchrg = mtRntchrg;
        this.enty = enty;
        this.surlus = surlus;
    }
}
