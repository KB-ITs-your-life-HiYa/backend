-- 금융 데이터.
--   account      계좌
--   transaction  거래 내역
--
-- 마이데이터 연동 전까지는 시드로 채워 넣는다.

CREATE TABLE account (
    id                 BIGSERIAL   PRIMARY KEY,
    member_id          BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,

    bank_name          VARCHAR(50) NOT NULL,
    account_type       VARCHAR(20) NOT NULL,   -- 입출금 / 적금
    balance            BIGINT      NOT NULL DEFAULT 0,

    -- 잔액 기준 시각. 없으면 이 잔액이 언제 것인지 알 수 없다.
    balance_updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_account_type
        CHECK (account_type IN ('DEPOSIT', 'SAVINGS')),

    -- transaction 이 (account_id, member_id) 조합을 참조할 수 있게 한다.
    -- id 가 이미 PK 라 이 UNIQUE 는 중복 방지 목적이 아니라 참조 대상을 만드는 목적이다.
    CONSTRAINT uq_account_id_member UNIQUE (id, member_id)
);

CREATE INDEX ix_account_member ON account (member_id);

COMMENT ON TABLE  account IS '계좌';
COMMENT ON COLUMN account.account_type       IS 'DEPOSIT 입출금 / SAVINGS 적금';
COMMENT ON COLUMN account.balance_updated_at IS '잔액 기준 시각';


-- transaction 은 Postgres 예약어가 아니라 따옴표 없이 쓸 수 있다.
CREATE TABLE transaction (
    id            BIGSERIAL    PRIMARY KEY,

    -- account 를 통해 알 수 있지만 조회 편의로 둔다.
    -- 저장할 때 반드시 account 에서 읽어 넣을 것. 직접 받으면 두 값이 어긋난다.
    member_id     BIGINT       NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    account_id    BIGINT       NOT NULL,

    txn_date      DATE         NOT NULL,
    txn_type      VARCHAR(10)  NOT NULL,   -- INCOME / EXPENSE
    amount        BIGINT       NOT NULL,   -- 항상 양수. 방향은 txn_type 이 정한다
    merchant_name VARCHAR(100),            -- money_schedule.match_keyword 와 대조
    category      VARCHAR(20),

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_transaction_type
        CHECK (txn_type IN ('INCOME', 'EXPENSE')),
    -- 음수 금액을 허용하면 방향이 두 곳에서 정해져 합계가 어긋난다.
    CONSTRAINT ck_transaction_amount_positive
        CHECK (amount > 0),
    CONSTRAINT ck_transaction_category
        CHECK (category IS NULL
               OR category IN ('HOUSING_UTILITY', 'FOOD', 'TRANSPORT',
                               'LIVING_MEDICAL', 'LEISURE_SHOPPING', 'SAVINGS', 'ETC')),

    -- account_id 만 참조하면 member_id 를 아무 값이나 넣어도 통과한다.
    -- 남의 계좌 거래가 내 지출 합계에 섞이게 되므로, 두 컬럼을 묶어서 참조한다.
    CONSTRAINT fk_transaction_account_member
        FOREIGN KEY (account_id, member_id) REFERENCES account (id, member_id) ON DELETE CASCADE
);

-- 월 지출 합계는 (회원, 기간) 으로 조회한다. 예산 화면이 켜질 때마다 도는 쿼리다.
CREATE INDEX ix_transaction_member_date ON transaction (member_id, txn_date);
CREATE INDEX ix_transaction_account     ON transaction (account_id);

COMMENT ON TABLE  transaction IS '거래 내역';
COMMENT ON COLUMN transaction.amount    IS '항상 양수. 수입/지출 구분은 txn_type';
COMMENT ON COLUMN transaction.member_id IS 'account 에서 읽어 넣는다. 직접 받지 말 것';
