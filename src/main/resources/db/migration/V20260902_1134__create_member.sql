-- 회원 기본 테이블.
--
-- 모든 도메인이 참조하는 기반 테이블이라 가장 먼저 만든다.
-- 회원 도메인의 나머지(member_survey, member_survey_tag, counselor)는
-- 별도 마이그레이션으로 추가한다.
--
-- 테이블명이 user 가 아닌 이유: user 는 Postgres 예약어라 따옴표 없이 쓸 수 없다.
-- 인증은 Spring Security + 자체 JWT.

CREATE TABLE member (
    id                      BIGSERIAL    PRIMARY KEY,

    email                   VARCHAR(255) NOT NULL UNIQUE,   -- 로그인 ID
    password_hash           VARCHAR(60)  NOT NULL,          -- BCrypt 해시는 항상 60자

    birth_date              DATE         NOT NULL,          -- 연령 요건 판정
    gender                  VARCHAR(10)  NOT NULL,          -- MALE / FEMALE / OTHER

    region_code             CHAR(2)      NOT NULL,          -- 현재(예정) 거주 시도 코드
    region_sigungu          VARCHAR(30),                    -- 시군구 이름

    protection_status       VARCHAR(20)  NOT NULL,          -- IN_CARE / ENDED
    protection_end_date     DATE,                           -- 자립준비청년 5년 이내 판정 기준
    protection_expected_end DATE,                           -- IN_CARE 일 때 종료 예정 (선택)
    protection_type         VARCHAR(20),                    -- FACILITY / FOSTER_CARE
    home_region_code        CHAR(2),                        -- 보호종료 당시 시도. 정착금 산정용

    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_member_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_member_protection_status
        CHECK (protection_status IN ('IN_CARE', 'ENDED')),
    CONSTRAINT ck_member_protection_type
        CHECK (protection_type IS NULL OR protection_type IN ('FACILITY', 'FOSTER_CARE')),
    -- 보호종료 상태면 종료일이 반드시 있어야 한다.
    -- 없으면 자립준비청년 판정도, D-1825 계산도 할 수 없다.
    CONSTRAINT ck_member_end_date_required
        CHECK (protection_status <> 'ENDED' OR protection_end_date IS NOT NULL)
);

COMMENT ON TABLE  member IS '회원 (자립준비청년)';
COMMENT ON COLUMN member.protection_end_date IS '보호종료일. D-1825 기준점이자 자립준비청년 판정 기준';
COMMENT ON COLUMN member.home_region_code    IS '보호종료 당시 시도. 현재 거주지(region_code)와 다를 수 있다';

-- 로그인은 email 로 조회한다. UNIQUE 제약이 인덱스를 겸하므로 별도 인덱스는 두지 않는다.
