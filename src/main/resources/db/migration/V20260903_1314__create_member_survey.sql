-- 회원 도메인의 나머지 테이블.
--   member_survey      정책 매칭 설문 (회원당 0~1행)
--   member_survey_tag  설문의 다중선택 태그
--   counselor          담당자 계정
--
-- 상태·구분 값은 Postgres enum 타입 대신 VARCHAR + CHECK 로 둔다.
-- enum 타입은 값 하나 추가에도 타입을 바꿔야 하고 삭제가 사실상 불가능하다.
-- CHECK 는 제약만 갈아끼우면 되고, Java 쪽은 어차피 @Enumerated(STRING) 으로 매핑한다.


-- 가입 후 별도로 받는 정보. 안 한 회원도 있으므로 member 와 나눈다.
-- 주거 자격 판정도 이 테이블을 읽는다(소득·수급자·주거형태).
CREATE TABLE member_survey (
    -- PK 이자 FK. 회원당 한 행만 존재한다.
    member_id            BIGINT       PRIMARY KEY REFERENCES member (id) ON DELETE CASCADE,

    household_size       INT,                        -- 소득 기준이 가구원 수별로 다르다
    income_pct_bracket   INT,                        -- 기준중위소득 구간
    is_benefit_recipient BOOLEAN,                    -- 수급자 여부. 영구임대·매입임대 1순위 판정
    employment_status    VARCHAR(20),
    housing_type         VARCHAR(20),

    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_member_survey_household_size
        CHECK (household_size IS NULL OR household_size BETWEEN 1 AND 10),
    CONSTRAINT ck_member_survey_income_bracket
        CHECK (income_pct_bracket IS NULL
               OR income_pct_bracket IN (32, 48, 50, 60, 100, 120, 150, 999)),
    CONSTRAINT ck_member_survey_employment_status
        CHECK (employment_status IS NULL
               OR employment_status IN ('EMPLOYED', 'SEEKING', 'STUDENT', 'UNEMPLOYED', 'SELF_EMPLOYED')),
    CONSTRAINT ck_member_survey_housing_type
        CHECK (housing_type IS NULL
               OR housing_type IN ('OWNED', 'JEONSE', 'MONTHLY_RENT', 'FREE',
                                   'SELF_RELIANCE_HOUSE', 'PUBLIC_RENTAL'))
);

COMMENT ON TABLE  member_survey IS '정책 매칭 설문. 주거 자격 판정도 이 값을 읽는다';
COMMENT ON COLUMN member_survey.income_pct_bracket IS '기준중위소득 구간(%). 999 는 초과 또는 모름';


-- 다중 선택 값. 배열 컬럼 대신 테이블로 둔다.
-- subsidy 쪽 태그와 같은 어휘를 써야 매칭이 조인 한 번으로 끝난다.
CREATE TABLE member_survey_tag (
    member_id BIGINT      NOT NULL REFERENCES member_survey (member_id) ON DELETE CASCADE,
    tag       VARCHAR(30) NOT NULL,

    -- 같은 태그를 두 번 넣을 수 없다.
    PRIMARY KEY (member_id, tag),

    CONSTRAINT ck_member_survey_tag
        CHECK (tag IN ('SINGLE_PARENT', 'MULTICULTURAL', 'DISABILITY',
                       'SEVERE_ILLNESS', 'NORTH_KOREAN_DEFECTOR', 'GRANDPARENT_FAMILY'))
);

COMMENT ON TABLE member_survey_tag IS '회원 특성 태그(한부모·다문화 등). 지원금 매칭 조건과 대조한다';


-- 자립지원전담기관 등의 담당자 계정.
-- member 와 합치지 않은 이유: member 는 birth_date·protection_status·region_code 가
-- NOT NULL 이라, 담당자에게 의미 없는 그 제약을 전부 풀어야 한다.
CREATE TABLE counselor (
    id            BIGSERIAL    PRIMARY KEY,

    email         VARCHAR(255) NOT NULL UNIQUE,   -- 로그인 ID
    password_hash VARCHAR(60)  NOT NULL,          -- BCrypt 해시는 항상 60자
    name          VARCHAR(50)  NOT NULL,
    phone         VARCHAR(20),
    organization  VARCHAR(100),
    region_code   VARCHAR(2),                     -- 담당 지역 시도
    is_active     BOOLEAN      NOT NULL DEFAULT true,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE counselor IS '담당자 계정. member 와 테이블이 달라 이메일 중복은 DB 가 막지 못한다';
