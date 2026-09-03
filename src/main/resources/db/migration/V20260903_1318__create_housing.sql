-- 독립지원(주거) 도메인.
--   housing_notice          임대주택 공고  (캘린더의 점 하나 = 이 테이블 한 줄)
--   housing_notice_unit     공고에 딸린 단지·지역
--   housing_interest        관심 공고
--   housing_checklist       체크리스트
--   housing_checklist_item  체크리스트 항목
--
-- 공고는 마이홈포털 API 에서 수집한다. 컬럼명은 API 필드명을 그대로 따른다 --
-- 원문과 대조하기 쉽고, 수집 코드에서 이름을 바꿔 옮기다 생기는 실수를 없앤다.


CREATE TABLE housing_notice (
    id                BIGSERIAL   PRIMARY KEY,

    pblanc_id         VARCHAR(20) NOT NULL UNIQUE,   -- 마이홈포털 공고 식별자
    pblanc_nm         TEXT        NOT NULL,          -- 공고명
    suply_instt_nm    VARCHAR(50),                   -- LH · 부산도시공사 …
    house_ty_nm       VARCHAR(30),                   -- 아파트 · 다가구주택
    suply_ty_nm       VARCHAR(30),                   -- 행복주택 · 국민임대 · 매입임대 …

    -- 우리 분류. 캘린더 정렬 기준이라 저장한다.
    -- 매번 공고명을 검색하면 인덱스를 못 탄다.
    target_type       VARCHAR(20) NOT NULL DEFAULT 'GENERAL',

    sttus_nm          VARCHAR(20),                   -- 일반공고 / 정정공고
    before_pblanc_id  VARCHAR(20),                   -- 정정공고가 대체하는 공고

    -- 대체된 공고. 삭제하지 않고 플래그로 둔다.
    -- 삭제하면 그 공고를 관심 등록한 사용자 데이터가 영향받는다.
    superseded        BOOLEAN     NOT NULL DEFAULT false,

    rcrit_pblanc_de   DATE,                          -- 공고일
    begin_de          DATE,                          -- 접수 시작
    end_de            DATE,                          -- 접수 마감
    przwner_presnatn_de DATE,                        -- 당첨자 발표일

    refrnc            TEXT,                          -- 문의처
    url               TEXT,
    pc_url            TEXT,

    -- 배치가 도는지 확인하는 기준.
    collected_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_housing_notice_target_type
        CHECK (target_type IN ('SELF_RELIANCE', 'YOUTH', 'GENERAL')),
    -- 마감이 시작보다 빠른 데이터가 들어오면 캘린더가 깨진다.
    CONSTRAINT ck_housing_notice_period
        CHECK (begin_de IS NULL OR end_de IS NULL OR begin_de <= end_de)
);

-- 캘린더는 "이번 달에 걸치는 공고" 를 조회한다. 대체된 공고는 제외한다.
CREATE INDEX ix_housing_notice_period ON housing_notice (end_de, begin_de) WHERE superseded = false;
CREATE INDEX ix_housing_notice_target ON housing_notice (target_type);

COMMENT ON TABLE  housing_notice IS '임대주택 공고. 캘린더의 점 하나가 이 테이블 한 줄';
COMMENT ON COLUMN housing_notice.target_type IS '우리 분류. 자립준비청년 공고를 1순위로 노출하는 기준';
COMMENT ON COLUMN housing_notice.superseded  IS '정정공고에 대체됨. 캘린더에서 제외';


-- 공고 하나에 단지가 평균 3개.
-- API 가 308행을 주는데 실제 공고는 104건이다.
CREATE TABLE housing_notice_unit (
    -- 서로게이트 PK 가 필수다. house_sn 이 유니크하지 않아 자연키를 만들 수 없다
    -- (같은 공고 안에서 0 이 여러 번 나오고, 전체의 54% 가 0 이다).
    id           BIGSERIAL    PRIMARY KEY,
    notice_id    BIGINT       NOT NULL REFERENCES housing_notice (id) ON DELETE CASCADE,

    house_sn     INT,                        -- API 의 단지 번호. 유니크하지 않다
    hsmp_nm      VARCHAR(100),               -- 단지명
    brtc_nm      VARCHAR(30),                -- 시도 이름. 지역 필터 기준
    signgu_nm    VARCHAR(30),                -- 시군구
    full_adres   TEXT,
    heat_mthd_nm VARCHAR(30),                -- 난방방식

    tot_hshld_co INT,                        -- 총 세대수
    sum_suply_co INT,                        -- 공급호수

    rent_gtn     BIGINT,                     -- 보증금
    mt_rntchrg   BIGINT,                     -- 월세
    enty         BIGINT,                     -- 계약금
    surlus       BIGINT                      -- 잔금
);

-- 공고 상세 화면이 단지 목록을 조회한다.
CREATE INDEX ix_housing_unit_notice ON housing_notice_unit (notice_id);
-- 지역 필터. 회원의 거주 시도로 거른다.
CREATE INDEX ix_housing_unit_region ON housing_notice_unit (brtc_nm);

COMMENT ON TABLE  housing_notice_unit IS '공고에 딸린 단지·지역. 보증금·월세는 단지마다 다르다';
COMMENT ON COLUMN housing_notice_unit.house_sn IS 'API 의 단지 번호. 유니크하지 않아 PK 로 쓸 수 없다';


CREATE TABLE housing_interest (
    id         BIGSERIAL   PRIMARY KEY,
    member_id  BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    notice_id  BIGINT      NOT NULL REFERENCES housing_notice (id) ON DELETE CASCADE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 같은 공고를 두 번 담을 수 없다.
    CONSTRAINT uq_housing_interest UNIQUE (member_id, notice_id)
);

COMMENT ON TABLE housing_interest IS '관심 공고';


-- "종류별 1개, 최대 3개" 를 DB 가 강제한다.
-- 종류가 3개뿐인데 종류별 1개씩만 되므로 최대 3개가 된다.
-- 코드로 세지 않아도 되고, 버튼을 빠르게 두 번 눌러도 막힌다.
CREATE TABLE housing_checklist (
    id            BIGSERIAL   PRIMARY KEY,
    member_id     BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,

    template_type VARCHAR(20) NOT NULL,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_housing_checklist_template
        CHECK (template_type IN ('HOUSE_HUNTING', 'MOVING', 'MOVE_IN')),
    CONSTRAINT uq_housing_checklist UNIQUE (member_id, template_type)
);

COMMENT ON TABLE housing_checklist IS '체크리스트. 종류별 1개라 최대 3개';


CREATE TABLE housing_checklist_item (
    id           BIGSERIAL   PRIMARY KEY,
    checklist_id BIGINT      NOT NULL REFERENCES housing_checklist (id) ON DELETE CASCADE,

    content      TEXT        NOT NULL,
    due_date     DATE,
    memo         TEXT,
    done         BOOLEAN     NOT NULL DEFAULT false,   -- 진행률 계산용

    -- 순서가 의미를 가진다 (등본 발급 -> 계약 -> 전입신고).
    -- id 순서에 의존하면 나중에 중간 삽입을 못 한다.
    sort_order   INT         NOT NULL DEFAULT 0,

    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_housing_item_checklist ON housing_checklist_item (checklist_id, sort_order);

COMMENT ON TABLE  housing_checklist_item IS '체크리스트 항목';
COMMENT ON COLUMN housing_checklist_item.sort_order IS '표시 순서. id 순서에 의존하지 않는다';
