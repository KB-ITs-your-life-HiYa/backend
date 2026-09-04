-- habit_topic 을 카테고리(신용대출/저축투자/소비습관)로 묶기 위한 테이블.
--
-- 기존에는 habit_topic 한 건이 곧 "신용, 대출" / "저축, 투자" / "소비습관" 카드 하나였다.
-- 각 주제 아래에 세부 토픽이 여러 개 생기면서(예: 저축,투자 → 저축이란 / 저축 기본
-- 이해 / 금리,이자 이해 / 안전성,보호 제도) 2단계 구조가 필요해졌다.
--
--   habit_topic_category : 최상위 카테고리. 놀이 탭 "금융 상식 쑥쑥"에서 처음 보여주는 카드
--   habit_topic.category_id : 각 세부 토픽이 속한 카테고리를 가리킨다

CREATE TABLE habit_topic_category (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(50)  NOT NULL,
    subtitle    VARCHAR(100) NOT NULL,
    icon        VARCHAR(50)  NOT NULL,      -- MaterialCommunityIcons 아이콘 이름
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE habit_topic_category IS '금융 상식 쑥쑥 — 최상위 카테고리 (신용/대출, 저축/투자, 소비습관)';

ALTER TABLE habit_topic ADD COLUMN category_id BIGINT REFERENCES habit_topic_category(id);

COMMENT ON COLUMN habit_topic.category_id IS
    '이 토픽이 속한 카테고리. 로컬 시드가 항상 채워주므로 NOT NULL 로 걸지는 않았다';

CREATE INDEX ix_habit_topic_category_id ON habit_topic (category_id);
