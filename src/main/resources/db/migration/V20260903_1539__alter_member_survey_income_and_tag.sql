-- PR #14 리뷰 반영.
--   1. 소득구간 999 의 의미를 "초과" 로 좁히고, "모름" 은 NULL 로 구분한다
--   2. 회원 특성 태그에 다자녀(MULTI_CHILD) 추가


-- 1. 소득구간의 "초과" 와 "모름" 을 구분한다.
--
-- 컬럼 자체는 NULL 을 허용하고 있어 스키마 변경은 필요 없다.
-- 999 하나에 두 의미를 담아두면 정책 매칭에서 구분할 수 없다 --
-- 소득이 많아 대상이 아닌 사람과, 아직 답하지 않아 판정을 미뤄야 하는 사람은 다르다.
--
--   999   150% 초과 (소득 요건 미충족이 확정)
--   NULL  아직 모름 (설문 미응답. 판정 보류)
COMMENT ON COLUMN member_survey.income_pct_bracket
    IS '기준중위소득 구간(%). 999 는 150% 초과. 모르면 NULL 로 두고 판정을 보류한다';


-- 2. 다자녀 태그 추가.
--
-- CHECK 제약은 값 목록을 바꿀 때 통째로 교체한다.
-- subsidy 쪽에서 같은 어휘를 쓰기로 해서 여기에 먼저 추가한다.
ALTER TABLE member_survey_tag DROP CONSTRAINT ck_member_survey_tag;

ALTER TABLE member_survey_tag ADD CONSTRAINT ck_member_survey_tag
    CHECK (tag IN ('SINGLE_PARENT', 'MULTICULTURAL', 'DISABILITY', 'MULTI_CHILD',
                   'SEVERE_ILLNESS', 'NORTH_KOREAN_DEFECTOR', 'GRANDPARENT_FAMILY'));

COMMENT ON TABLE member_survey_tag
    IS '회원 특성 태그. subsidy 쪽 대상 조건과 같은 어휘를 쓴다';
