-- 놀이 탭(금융습관 트레이닝) 도메인 테이블.
--
-- 하루 1문제 퀴즈를 맞히면 퍼즐 조각을 모으는 구조.
--   habit_quiz / habit_quiz_option   : 문제 뱅크 (정답은 서버만 안다, 클라이언트에 노출 금지)
--   habit_quiz_answer                : 회원별 응답 로그. (member_id, answered_date) 유니크로
--                                       "하루 1회" 제한을 DB 레벨에서도 강제
--   habit_puzzle_set                 : 퍼즐 테마 세트 (여름 바다, 가을 피크닉 ...). 이미지 파일
--                                       자체는 프론트 정적 에셋(assets/puzzles)으로 관리하고,
--                                       여긴 메타데이터(제목, 에셋 키, 진행 순서)만 둠
--   habit_puzzle_progress            : 회원×세트별 조각 수집 현황. sort_order 순서대로
--                                       한 세트씩 진행 (이전 세트를 완성해야 다음 세트 시작)
--   habit_topic                      : "금융 상식 쑥쑥" 읽을거리

CREATE TABLE habit_quiz (
    id          BIGSERIAL   PRIMARY KEY,
    question    TEXT        NOT NULL,
    explanation TEXT,                      -- 정답 여부와 상관없이 답변 후 보여줄 해설
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE habit_quiz IS '금융습관 퀴즈 문제 뱅크';

CREATE TABLE habit_quiz_option (
    id          BIGSERIAL   PRIMARY KEY,
    quiz_id     BIGINT      NOT NULL REFERENCES habit_quiz(id),
    label       TEXT        NOT NULL,
    is_correct  BOOLEAN     NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0
);

COMMENT ON TABLE habit_quiz_option IS '퀴즈 보기. is_correct 는 서버 판정용이며 클라이언트 응답에 절대 포함하지 않는다';

CREATE INDEX ix_habit_quiz_option_quiz_id ON habit_quiz_option (quiz_id);

CREATE TABLE habit_quiz_answer (
    id                  BIGSERIAL   PRIMARY KEY,
    member_id           BIGINT      NOT NULL REFERENCES member(id),
    quiz_id             BIGINT      NOT NULL REFERENCES habit_quiz(id),
    selected_option_id  BIGINT      NOT NULL REFERENCES habit_quiz_option(id),
    is_correct          BOOLEAN     NOT NULL,
    answered_date       DATE        NOT NULL,          -- Asia/Seoul 기준 날짜. 하루 1회 제한 기준
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_habit_quiz_answer_member_date UNIQUE (member_id, answered_date)
);

COMMENT ON TABLE habit_quiz_answer IS '회원별 퀴즈 응답 로그. member_id+answered_date 유니크로 하루 1회 제한';

CREATE TABLE habit_puzzle_set (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(50)  NOT NULL,          -- 예: '봄 벚꽃'
    asset_key    VARCHAR(50)  NOT NULL UNIQUE,    -- 프론트 assets/puzzles/<asset_key>.png 매핑 키
    total_pieces INT          NOT NULL DEFAULT 16,
    sort_order   INT          NOT NULL UNIQUE,    -- 수집 진행 순서. 이 값만 바꾸면 순서 재배치 가능
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE habit_puzzle_set IS '퍼즐 테마 세트. 이미지 파일 자체는 프론트 정적 에셋으로 관리하고, 여긴 메타데이터만 둔다';
COMMENT ON COLUMN habit_puzzle_set.asset_key IS '프론트가 로컬 이미지를 찾는 키. 실제 파일 경로는 프론트와 합의된 규칙(assets/puzzles/<asset_key>.png)을 따른다';
COMMENT ON COLUMN habit_puzzle_set.sort_order IS '작아질수록 먼저 진행된다. 순서를 바꾸고 싶으면 이 값만 갱신하면 된다';

CREATE TABLE habit_puzzle_progress (
    id                BIGSERIAL   PRIMARY KEY,
    member_id         BIGINT      NOT NULL REFERENCES member(id),
    puzzle_set_id     BIGINT      NOT NULL REFERENCES habit_puzzle_set(id),
    collected_pieces  INT         NOT NULL DEFAULT 0,
    completed_at      TIMESTAMPTZ,                     -- 그 세트를 다 모은 시각. 완성 축하 모달 트리거 기준
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_habit_puzzle_progress_member_set UNIQUE (member_id, puzzle_set_id)
);

COMMENT ON TABLE habit_puzzle_progress IS '회원×퍼즐 세트별 수집 현황. 세트를 sort_order 순서대로 하나씩 진행한다';

CREATE INDEX ix_habit_puzzle_progress_member_id ON habit_puzzle_progress (member_id);

CREATE TABLE habit_topic (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(50)  NOT NULL,
    subtitle    VARCHAR(100) NOT NULL,
    icon        VARCHAR(50)  NOT NULL,      -- MaterialCommunityIcons 아이콘 이름
    body        TEXT         NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE habit_topic IS '금융 상식 쑥쑥 — 토픽 읽을거리';
