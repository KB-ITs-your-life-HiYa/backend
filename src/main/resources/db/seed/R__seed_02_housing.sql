-- 데모용 자립준비청년 전용 공고.
--
-- 【왜 필요한가】
--   마이홈포털 API 는 "현재 접수 중인" 공고만 돌려준다.
--   실측 시점(2026-09-03)에 진행 중인 자립준비청년 전용 공고가 0건이라
--   캘린더 1순위 정렬을 보여줄 데이터가 없었다.
--
--   자립준비청년 전용 공고 자체는 실재한다. 예를 들어 아래 공고가 있었다.
--     "2025년 자립준비청년(청년 유형) 전세임대 입주자 수시모집"  (pblancId 17490)
--     https://www.myhome.go.kr/hws/portal/sch/selectRsdtRcritNtcDetailView.do?pblancId=17490
--   이 시드는 그 공고를 본떠 만들되 날짜만 현재 기준으로 둔 것이다.
--
-- 【수집과 충돌하지 않는 이유】
--   pblanc_id 를 'SEED-' 로 시작하게 두었다.
--   수집기는 API 가 준 pblancId 로만 upsert 하므로 이 행들을 건드리지 않는다.
--   지울 때도 'SEED-%' 만 지우므로 수집한 공고가 날아가지 않는다.
--
-- 【날짜를 상대값으로 두는 이유】
--   고정 날짜로 넣으면 시간이 지나면서 캘린더에서 사라진다.
--   CURRENT_DATE 기준이면 언제 실행해도 이번 달에 보인다.

DELETE FROM housing_notice_unit
 WHERE notice_id IN (SELECT id FROM housing_notice WHERE pblanc_id LIKE 'SEED-%');
DELETE FROM housing_notice WHERE pblanc_id LIKE 'SEED-%';


-- 공고 1 — 상시 모집. 접수기간이 길어 캘린더에서 "상시 모집" 으로 분류된다
INSERT INTO housing_notice (
    pblanc_id, pblanc_nm, suply_instt_nm, house_ty_nm, suply_ty_nm,
    target_type, sttus_nm, superseded,
    rcrit_pblanc_de, begin_de, end_de, przwner_presnatn_de,
    refrnc, url, pc_url, collected_at
) VALUES (
    'SEED-SR-001',
    '2026년 자립준비청년(청년 유형) 전세임대 입주자 수시모집',
    'LH', '다가구주택', '전세임대',
    'SELF_RELIANCE', '일반공고', false,
    CURRENT_DATE - 30, CURRENT_DATE - 30, CURRENT_DATE + 90, NULL,
    'LH 콜센터 : 1600-1004 (평일 : 09:00 ~ 18:00)',
    'https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026',
    'https://www.myhome.go.kr/hws/portal/sch/selectRsdtRcritNtcDetailView.do?pblancId=17490',
    now()
);

-- 공고 2 — 접수기간이 짧다. 캘린더에 시작·마감 점이 둘 다 찍힌다
INSERT INTO housing_notice (
    pblanc_id, pblanc_nm, suply_instt_nm, house_ty_nm, suply_ty_nm,
    target_type, sttus_nm, superseded,
    rcrit_pblanc_de, begin_de, end_de, przwner_presnatn_de,
    refrnc, url, pc_url, collected_at
) VALUES (
    'SEED-SR-002',
    '2026년 보호종료아동 우선공급 매입임대주택 예비입주자 모집공고',
    'LH', '아파트', '매입임대',
    'SELF_RELIANCE', '일반공고', false,
    CURRENT_DATE - 3, CURRENT_DATE + 4, CURRENT_DATE + 11, CURRENT_DATE + 32,
    'LH 콜센터 : 1600-1004 (평일 : 09:00 ~ 18:00)',
    'https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do?mi=1026',
    'https://www.myhome.go.kr/hws/portal/sch/selectRsdtRcritNtcList.do',
    now()
);


-- 단지. demo2 의 거주지가 인천(28)이므로 인천 단지를 넣어 지역 필터에 걸리게 한다.
INSERT INTO housing_notice_unit (
    notice_id, house_sn, hsmp_nm, brtc_nm, signgu_nm, full_adres, heat_mthd_nm,
    tot_hshld_co, sum_suply_co, rent_gtn, mt_rntchrg, enty, surlus
)
SELECT id, 1, '전세임대(지역 내 물색)', '인천광역시', '미추홀구',
       '인천광역시 미추홀구 (입주자가 직접 물색)', NULL,
       NULL, 100, 1000000, 0, 0, 0
  FROM housing_notice WHERE pblanc_id = 'SEED-SR-001';

INSERT INTO housing_notice_unit (
    notice_id, house_sn, hsmp_nm, brtc_nm, signgu_nm, full_adres, heat_mthd_nm,
    tot_hshld_co, sum_suply_co, rent_gtn, mt_rntchrg, enty, surlus
)
SELECT id, 1, '인천주안 A-2BL', '인천광역시', '미추홀구',
       '인천광역시 미추홀구 주안동 111', '지역난방',
       420, 12, 3200000, 152000, 320000, 2880000
  FROM housing_notice WHERE pblanc_id = 'SEED-SR-002';

INSERT INTO housing_notice_unit (
    notice_id, house_sn, hsmp_nm, brtc_nm, signgu_nm, full_adres, heat_mthd_nm,
    tot_hshld_co, sum_suply_co, rent_gtn, mt_rntchrg, enty, surlus
)
SELECT id, 2, '수원권선 B-1BL', '경기도', '수원시 권선구',
       '경기도 수원시 권선구 권선동 222', '개별난방',
       380, 8, 2800000, 138000, 280000, 2520000
  FROM housing_notice WHERE pblanc_id = 'SEED-SR-002';
