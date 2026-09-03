-- 생활비 도메인.
--   monthly_budget  사용자가 정한 월 예산
--   money_schedule  매달 반복되는 돈의 약속 (설정)
--   money_cycle     그 약속의 매달 상태


-- 사용자가 설정한 값만 담는다.
-- 지출 합계·지출률·상태는 저장하지 않고 매번 transaction 에서 계산한다.
-- 저장하면 재계산이 밀렸을 때 화면 숫자가 실제와 달라진다.
CREATE TABLE monthly_budget (
    id           BIGSERIAL   PRIMARY KEY,
    member_id    BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,

    budget_month DATE        NOT NULL,   -- 대상 월. 매월 1일로 저장
    total_amount BIGINT      NOT NULL,

    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 같은 달 예산이 두 개 생기는 것을 막는다.
    CONSTRAINT uq_monthly_budget UNIQUE (member_id, budget_month),
    -- 매월 1일이 아닌 값이 들어오면 월별 조회가 어긋난다.
    CONSTRAINT ck_monthly_budget_first_day
        CHECK (EXTRACT(DAY FROM budget_month) = 1),
    CONSTRAINT ck_monthly_budget_amount_positive
        CHECK (total_amount > 0)
);

COMMENT ON TABLE  monthly_budget IS '월 예산. 사용자가 정한 값만 담는다';
COMMENT ON COLUMN monthly_budget.budget_month IS '대상 월. 항상 그 달 1일';


-- "청년내일저축계좌, 매달 25일에 20만원 나감"
-- "카페 알바, 매달 10일에 들어옴"
--
-- 나가는 돈과 들어오는 돈을 한 테이블에 둔 이유: 하는 일이 같다.
-- 예정일에 거래를 찾아 확인하고, 없으면 케어 신호를 켠다. 방향만 다르다.
CREATE TABLE money_schedule (
    id              BIGSERIAL    PRIMARY KEY,
    member_id       BIGINT       NOT NULL REFERENCES member (id) ON DELETE CASCADE,

    direction       VARCHAR(3)   NOT NULL,   -- OUT 나감 / IN 들어옴
    type            VARCHAR(20)  NOT NULL,   -- OUT: SAVINGS·RENT·TELECOM·UTILITY / IN: SALARY·PART_TIME·OTHER_REGULAR
    name            VARCHAR(100) NOT NULL,   -- 표시용 이름
    expected_amount BIGINT,                  -- 소득은 변동할 수 있어 NULL 허용
    expected_day    SMALLINT     NOT NULL,   -- 매월 예정일
    match_keyword   VARCHAR(100),            -- transaction.merchant_name 매칭용
    is_active       BOOLEAN      NOT NULL DEFAULT true,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_money_schedule_direction
        CHECK (direction IN ('OUT', 'IN')),
    CONSTRAINT ck_money_schedule_day
        CHECK (expected_day BETWEEN 1 AND 31),
    -- 방향과 종류가 맞아야 한다. IN 인데 RENT 같은 조합을 막는다.
    CONSTRAINT ck_money_schedule_direction_type
        CHECK ((direction = 'OUT' AND type IN ('SAVINGS', 'RENT', 'TELECOM', 'UTILITY'))
            OR (direction = 'IN'  AND type IN ('SALARY', 'PART_TIME', 'OTHER_REGULAR'))),
    -- 나갈 돈은 금액을 알아야 미납을 판정할 수 있다.
    CONSTRAINT ck_money_schedule_out_amount_required
        CHECK (direction <> 'OUT' OR expected_amount IS NOT NULL),

    -- money_cycle 이 (id, member_id) 조합을 참조할 수 있게 한다.
    -- id 가 이미 PK 이므로 중복 방지가 아니라 참조 대상을 만드는 목적이다.
    CONSTRAINT uq_money_schedule_id_member UNIQUE (id, member_id)
);

CREATE INDEX ix_money_schedule_member ON money_schedule (member_id, is_active);

COMMENT ON TABLE  money_schedule IS '매달 반복되는 돈의 약속(설정)';
COMMENT ON COLUMN money_schedule.expected_day IS '매월 예정일 1~31. 그 달에 없는 날이면 말일로 보정';


-- money_schedule 하나에 매달 한 줄씩 생긴다.
--
-- 설정과 상태를 나눈 이유: 합치면 이번 달만 알 수 있다.
-- 10월이 되면 9월에 놓쳤다는 사실이 덮어씌워진다.
-- 나눠두면 "올해 몇 번 놓쳤나" 를 셀 수 있고, 그게 위험도 판단의 근거가 된다.
CREATE TABLE money_cycle (
    id                     BIGSERIAL   PRIMARY KEY,
    schedule_id            BIGINT      NOT NULL,

    -- schedule 을 통해 알 수 있지만, care_signal 이 회원 일치를 검증할 수 있도록 둔다.
    -- 이 값이 schedule 의 주인과 다르면 남의 이상징후가 내 홈 화면에 뜬다.
    member_id              BIGINT      NOT NULL,

    cycle_month            DATE        NOT NULL,   -- 대상 월. 매월 1일
    expected_date          DATE        NOT NULL,   -- 그 달의 실제 예정일
    expected_amount        BIGINT,

    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- 확인된 실제 거래. 거래가 지워져도 사이클은 남겨야 하므로 SET NULL.
    matched_transaction_id BIGINT      REFERENCES transaction (id) ON DELETE SET NULL,
    actual_date            DATE,
    actual_amount          BIGINT,

    reminder_sent_at       TIMESTAMPTZ,            -- D-Day 알림 발송 시각

    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 배치가 재실행돼도 같은 달이 두 번 생기지 않는다.
    CONSTRAINT uq_money_cycle UNIQUE (schedule_id, cycle_month),
    CONSTRAINT ck_money_cycle_status
        CHECK (status IN ('PENDING', 'DONE', 'MISSED')),
    CONSTRAINT ck_money_cycle_first_day
        CHECK (EXTRACT(DAY FROM cycle_month) = 1),
    -- DONE 이면 무엇으로 확인했는지가 있어야 한다. 없으면 근거 없는 완료가 된다.
    CONSTRAINT ck_money_cycle_done_requires_match
        CHECK (status <> 'DONE' OR matched_transaction_id IS NOT NULL),

    -- schedule_id 만 참조하면 member_id 에 아무 값이나 넣어도 통과한다.
    -- 두 컬럼을 묶어 참조해 약속의 주인과 사이클의 주인이 반드시 같게 만든다.
    CONSTRAINT fk_money_cycle_schedule_member
        FOREIGN KEY (schedule_id, member_id) REFERENCES money_schedule (id, member_id) ON DELETE CASCADE,

    -- care_signal 이 (id, member_id) 조합을 참조할 수 있게 한다.
    CONSTRAINT uq_money_cycle_id_member UNIQUE (id, member_id)
);

-- 배치가 "예정일 지난 PENDING" 을 매일 훑는다.
CREATE INDEX ix_money_cycle_pending ON money_cycle (status, expected_date);

COMMENT ON TABLE  money_cycle IS '약속의 매달 상태. 배치가 매일 PENDING 을 확인해 DONE/MISSED 로 바꾼다';
COMMENT ON COLUMN money_cycle.matched_transaction_id IS 'DONE 판정의 근거가 된 실제 거래';
