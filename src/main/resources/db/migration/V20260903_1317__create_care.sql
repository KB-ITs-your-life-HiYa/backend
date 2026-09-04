-- 온라인 케어 도메인.
--   care_signal       이상징후
--   care_response     상담 대화
--   referral_request  담당자 연계
--
-- 도메인을 넘나드는 관계는 care_signal -> money_cycle 하나뿐이다.
-- 케어는 "돈의 약속이 지켜지지 않았다" 는 사실에서 출발하므로 그 연결은 필요하다.


CREATE TABLE care_signal (
    id                    BIGSERIAL   PRIMARY KEY,
    member_id             BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,

    -- 신호의 출처. 사이클이 지워지면 근거가 사라지므로 신호도 함께 지운다.
    money_cycle_id        BIGINT      NOT NULL,

    signal_type           VARCHAR(30) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    response_result       VARCHAR(30),             -- NULL 이면 아직 응답 없음
    classification_source VARCHAR(20),             -- RULE(버튼) / GEMINI(자유입력)

    detected_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    recheck_at            TIMESTAMPTZ,             -- 7일 후 재확인 예정
    rechecked_at          TIMESTAMPTZ,
    resolved_at           TIMESTAMPTZ,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_care_signal_type
        CHECK (signal_type IN ('MISSED_SAVING', 'MISSED_PAYMENT', 'INCOME_MISSING')),
    CONSTRAINT ck_care_signal_status
        CHECK (status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT ck_care_signal_response_result
        CHECK (response_result IS NULL
               OR response_result IN ('NORMAL_REASON', 'NEEDS_CARE')),
    CONSTRAINT ck_care_signal_classification_source
        CHECK (classification_source IS NULL
               OR classification_source IN ('RULE', 'GEMINI')),
    -- 해결됐다면 언제 해결됐는지가 있어야 한다.
    CONSTRAINT ck_care_signal_resolved_at
        CHECK (status <> 'RESOLVED' OR resolved_at IS NOT NULL),

    -- money_cycle_id 만 참조하면 member_id 에 아무 값이나 넣어도 통과한다.
    -- 그러면 홈 화면이 member_id 로 조회할 때 남의 이상징후가 뜬다. 개인정보 문제다.
    CONSTRAINT fk_care_signal_cycle_member
        FOREIGN KEY (money_cycle_id, member_id) REFERENCES money_cycle (id, member_id) ON DELETE CASCADE,

    -- referral_request 가 (id, member_id) 조합을 참조할 수 있게 한다.
    CONSTRAINT uq_care_signal_id_member UNIQUE (id, member_id)
);

-- 같은 사이클로 열려 있는 신호는 하나만.
-- 없으면 탐지 배치가 돌 때마다 신호가 쌓이고 사용자가 매일 같은 말을 듣는다.
CREATE UNIQUE INDEX uq_care_signal_open
    ON care_signal (money_cycle_id) WHERE status = 'OPEN';

-- 홈 화면이 "내 열린 신호" 를 조회한다.
CREATE INDEX ix_care_signal_member_status ON care_signal (member_id, status);

-- 7일 뒤 재확인 배치가 "기한이 된 열린 신호" 를 훑는다.
CREATE INDEX ix_care_signal_recheck ON care_signal (recheck_at) WHERE status = 'OPEN';

COMMENT ON TABLE  care_signal IS '이상징후. money_cycle 이 MISSED 가 되면 켜진다';
COMMENT ON COLUMN care_signal.response_result IS 'NORMAL_REASON 정상 사유 / NEEDS_CARE 연계 검토';


-- 신호 하나에 여러 줄이 쌓인다(멀티턴). created_at 순이 곧 대화 순서다.
--
-- AI 가 먼저 건네는 첫 메시지는 저장하지 않는다.
-- signal_type 별 고정 문구라 화면에서 다시 만든다.
CREATE TABLE care_response (
    id             BIGSERIAL   PRIMARY KEY,
    care_signal_id BIGINT      NOT NULL REFERENCES care_signal (id) ON DELETE CASCADE,

    input_type     VARCHAR(20) NOT NULL,   -- BUTTON / FREE_TEXT
    selected_value VARCHAR(30),            -- BUTTON 일 때
    input_text     TEXT,                   -- FREE_TEXT 일 때
    ai_reply       TEXT,                   -- Gemini 응답

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_care_response_input_type
        CHECK (input_type IN ('BUTTON', 'FREE_TEXT')),
    CONSTRAINT ck_care_response_selected_value
        CHECK (selected_value IS NULL
               OR selected_value IN ('ALREADY_DONE', 'DIFFICULT', 'CHANGED', 'LATER')),
    -- 입력 방식에 맞는 값이 반드시 있어야 한다. 빈 대화 줄을 막는다.
    CONSTRAINT ck_care_response_payload
        CHECK ((input_type = 'BUTTON'    AND selected_value IS NOT NULL)
            OR (input_type = 'FREE_TEXT' AND input_text     IS NOT NULL))
);

-- 대화 화면이 신호별로 시간순 조회한다.
CREATE INDEX ix_care_response_signal ON care_response (care_signal_id, created_at);

COMMENT ON TABLE  care_response IS '상담 대화. 버튼 선택 또는 자유입력 + Gemini 응답';


CREATE TABLE referral_request (
    id                    BIGSERIAL   PRIMARY KEY,
    member_id             BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    care_signal_id        BIGINT      NOT NULL,

    -- 배정 전에는 NULL. 담당자 계정이 지워져도 요청 이력은 남긴다.
    counselor_id          BIGINT      REFERENCES counselor (id) ON DELETE SET NULL,

    status                VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reason                VARCHAR(30) NOT NULL,
    risk_score_at_request SMALLINT,               -- 계산 방식은 아직 미정

    requested_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    contacted_at          TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,

    CONSTRAINT ck_referral_status
        CHECK (status IN ('REQUESTED', 'CONTACTED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_referral_reason
        CHECK (reason IN ('HIGH_RISK', 'UNRESOLVED_AFTER_7_DAYS')),
    CONSTRAINT ck_referral_score_range
        CHECK (risk_score_at_request IS NULL
               OR risk_score_at_request BETWEEN 0 AND 100),

    -- 연계 요청의 주인과 원인 신호의 주인이 반드시 같아야 한다.
    -- 어긋나면 담당자에게 엉뚱한 회원의 사연이 전달된다.
    CONSTRAINT fk_referral_signal_member
        FOREIGN KEY (care_signal_id, member_id) REFERENCES care_signal (id, member_id) ON DELETE CASCADE
);

-- 같은 신호로 진행 중인 연계는 하나만. 담당자에게 중복 요청이 가는 것을 막는다.
CREATE UNIQUE INDEX uq_referral_in_progress
    ON referral_request (care_signal_id) WHERE status IN ('REQUESTED', 'CONTACTED');

CREATE INDEX ix_referral_counselor ON referral_request (counselor_id, status);

COMMENT ON TABLE  referral_request IS '담당자 연계 요청';
COMMENT ON COLUMN referral_request.risk_score_at_request IS '요청 당시 위험점수. 산출 방식 미정';
