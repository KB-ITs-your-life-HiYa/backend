-- demo2 AI Care 시연용 금융 데이터 (PostgreSQL)
-- 원본: seed_demo2_finance.sql. 원본 거래는 모두 유지하고 세 거래를 추가함.
-- 수동 실행용: 전체 파일을 한 세션에서 실행. Flyway R__ 자동 실행 파일이 아님.
-- 전제: 프로젝트의 member/account/transaction/money_schedule/money_cycle/care 스키마 적용 완료.
-- 대상: member id=2 AND email='demo2@fledge.dev'. 회원 정보 자체는 수정하지 않음.
-- 관리 범위: 계좌 11~15, 일정 201~207, 아래에 명시된 거래 및 9월 시연 적금/급여 거래.
-- 재실행은 관리 일정의 사이클과 연결된 신호/상담/연계 요청을 초기화함.
-- 관리 범위 밖의 사이클이 삭제 대상 거래를 참조하면 전체 작업 중단.
-- 계좌 잔액은 원본 스냅샷 기준 보정값이며, 7월 이전 거래 내역은 제공하지 않음.
-- 9월 16~26일에는 수당 입금 외 추가 소비를 가정하지 않음.
-- 원본의 시급/근무시간은 정책 사실로 검증하지 않고 거래 금액만 보존함.
-- 시연 기준일은 서버 Clock 등에서 별도로 설정해야 함. 이 SQL은 서버 날짜를 변경하지 않음.
-- 09-23: 적금 D-Day, 0점 / 09-24: 적금 누락, 25점 / 09-26: 급여 누락 추가, 65점.
-- 10-01: 09-24 탐지 시각으로부터 7일 경과한 시각 이후 재확인.
-- 10월에도 65점을 유지한다는 뜻은 아님: 새 월 고정비는 별도 예정/거래 처리가 필요함.
-- 탐지는 반드시 txn_date <= 기준일 적용. 기존 OPEN 신호의 미해결 이전 월 사이클도 재확인.
-- 적금 계좌 입금 거래는 별도 생성하지 않음: 내부 이체를 소득으로 중복 집계하지 않기 위함.

BEGIN;
SET LOCAL TIME ZONE 'Asia/Seoul';

-- 두 시드 실행 간 중복 작업 방지. 실제 서비스 쓰기를 막는 유지보수 잠금은 아님.
SELECT pg_advisory_xact_lock(20260903, 2);

CREATE TEMP TABLE seed_demo2_account (
    id bigint PRIMARY KEY, bank_name varchar(50), account_type varchar(20), balance bigint
) ON COMMIT DROP;
INSERT INTO seed_demo2_account VALUES
  (11, 'KB국민', 'DEPOSIT', 2440600), -- 2,340,600 - 400,000 + 500,000
  (12, '신한',   'DEPOSIT',  615800),
  (13, 'KB국민', 'SAVINGS',  480000), -- 기존 자유적금
  (14, '우리',   'SAVINGS', 2700000), -- 기존 월 5만 원 정기적금
  (15, 'KB국민', 'SAVINGS',  400000); -- 시연용 월 20만 원 정기적금, 7~8월 두 번 납입

CREATE TEMP TABLE seed_demo2_schedule (
    id bigint PRIMARY KEY, direction varchar(3), type varchar(20), name varchar(100),
    expected_amount bigint, expected_day smallint, match_keyword varchar(100)
) ON COMMIT DROP;
INSERT INTO seed_demo2_schedule VALUES
  (201, 'OUT', 'SAVINGS',       'KB국민 시연 정기적금', 200000, 23, 'KB국민 시연 정기적금'),
  (202, 'OUT', 'SAVINGS',       '우리 정기적금',          50000, 15, '우리 정기적금'),
  (203, 'IN',  'PART_TIME',     '카페모디 급여',           NULL, 25, '카페모디 급여'),
  (204, 'IN',  'OTHER_REGULAR', '경기도청 자립수당',     500000, 20, '경기도청 자립수당'),
  (205, 'OUT', 'RENT',          'LH 임대료',             138700,  1, 'LH 임대료'),
  (206, 'OUT', 'TELECOM',       'KT알뜰폰',               29700,  5, 'KT알뜰폰'),
  (207, 'OUT', 'UTILITY',       '한국전력·도시가스',       38900,  5, '한국전력·도시가스');
-- 공과금 38,900원은 9월 기준. 과거 월 사이클을 생성할 경우 7월 44,200 / 8월 49,800 적용.
-- 급여는 변동 금액이므로 예정금액 NULL, 거래처와 IN 방향으로 매칭.

CREATE TEMP TABLE seed_demo2_txn (
    member_id bigint, account_id bigint, txn_date date, txn_type varchar(10), amount bigint,
    merchant_name varchar(100), category varchar(20)
) ON COMMIT DROP;

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 11, '2026-07-01', 'EXPENSE', 138700, 'LH 임대료',         'HOUSING_UTILITY'),
  (2, 11, '2026-07-05', 'EXPENSE',  44200, '한국전력·도시가스',  'HOUSING_UTILITY'),
  (2, 11, '2026-07-05', 'EXPENSE',  29700, 'KT알뜰폰',          'HOUSING_UTILITY'),
  (2, 11, '2026-07-10', 'EXPENSE',  62000, '티머니 충전',        'TRANSPORT'),
  (2, 11, '2026-07-15', 'EXPENSE',  50000, '우리 정기적금',      'SAVINGS'),
  (2, 11, '2026-07-22', 'EXPENSE', 100000, 'KB국민 자유적금',    'SAVINGS'),
  (2, 11, '2026-07-20', 'INCOME',  500000, '경기도청 자립수당',  NULL),
  (2, 11, '2026-07-25', 'INCOME',  412800, '카페모디 급여',      NULL);

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-07-02', 'EXPENSE',   7800, 'GS25',        'FOOD'),
  (2, 12, '2026-07-04', 'EXPENSE',  18600, '배달의민족',  'FOOD'),
  (2, 12, '2026-07-06', 'EXPENSE',  42300, '홈플러스',    'FOOD'),
  (2, 12, '2026-07-09', 'EXPENSE',   8500, '김밥천국',    'FOOD'),
  (2, 12, '2026-07-11', 'EXPENSE',  11700, '이마트24',    'FOOD'),
  (2, 12, '2026-07-13', 'EXPENSE',  21400, '배달의민족',  'FOOD'),
  (2, 12, '2026-07-15', 'EXPENSE',   9000, '용현동백반',  'FOOD'),
  (2, 12, '2026-07-17', 'EXPENSE',   6900, 'CU',          'FOOD'),
  (2, 12, '2026-07-19', 'EXPENSE',  54800, '이마트',      'FOOD'),
  (2, 12, '2026-07-21', 'EXPENSE',  19800, '배달의민족',  'FOOD'),
  (2, 12, '2026-07-24', 'EXPENSE',  57800, '홈플러스',    'FOOD'),
  (2, 12, '2026-07-25', 'EXPENSE',  11200, '맘스터치',    'FOOD'),
  (2, 12, '2026-07-27', 'EXPENSE',  15300, '신전떡볶이',  'FOOD'),
  (2, 12, '2026-07-30', 'EXPENSE',   8600, 'GS25',        'FOOD'),
  (2, 12, '2026-07-31', 'EXPENSE',  24700, '배달의민족',  'FOOD');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-07-05', 'EXPENSE',   5300, '스타벅스',    'LEISURE_SHOPPING'),
  (2, 12, '2026-07-12', 'EXPENSE',  13500, '넷플릭스',    'LEISURE_SHOPPING'),
  (2, 12, '2026-07-16', 'EXPENSE',  52400, '무신사',      'LEISURE_SHOPPING'),
  (2, 12, '2026-07-20', 'EXPENSE',  14000, 'CGV',         'LEISURE_SHOPPING'),
  (2, 12, '2026-07-26', 'EXPENSE',  61000, '쿠팡',        'LEISURE_SHOPPING'),
  (2, 12, '2026-07-29', 'EXPENSE',   6100, '스타벅스',    'LEISURE_SHOPPING');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-07-07', 'EXPENSE',  10400, '다이소',        'LIVING_MEDICAL'),
  (2, 12, '2026-07-14', 'EXPENSE',  24700, '올리브영',      'LIVING_MEDICAL'),
  (2, 12, '2026-07-22', 'EXPENSE',  19800, '쿠팡',          'LIVING_MEDICAL'),
  (2, 12, '2026-07-28', 'EXPENSE',  18400, '미추홀연합의원','LIVING_MEDICAL'),
  (2, 12, '2026-07-28', 'EXPENSE',   5000, '용현약국',      'LIVING_MEDICAL');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 11, '2026-08-01', 'EXPENSE', 138700, 'LH 임대료',         'HOUSING_UTILITY'),
  (2, 11, '2026-08-05', 'EXPENSE',  49800, '한국전력·도시가스',  'HOUSING_UTILITY'),
  (2, 11, '2026-08-05', 'EXPENSE',  29700, 'KT알뜰폰',          'HOUSING_UTILITY'),
  (2, 11, '2026-08-10', 'EXPENSE',  58000, '티머니 충전',        'TRANSPORT'),
  (2, 11, '2026-08-15', 'EXPENSE',  50000, '우리 정기적금',      'SAVINGS'),
  (2, 11, '2026-08-24', 'EXPENSE',  50000, 'KB국민 자유적금',    'SAVINGS'),
  (2, 11, '2026-08-20', 'INCOME',  500000, '경기도청 자립수당',  NULL),
  (2, 11, '2026-08-25', 'INCOME',  350880, '카페모디 급여',      NULL);

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-08-02', 'EXPENSE',   8200, 'GS25',        'FOOD'),
  (2, 12, '2026-08-04', 'EXPENSE',  44900, '홈플러스',    'FOOD'),
  (2, 12, '2026-08-06', 'EXPENSE',   8500, '김밥천국',    'FOOD'),
  (2, 12, '2026-08-08', 'EXPENSE',  20300, '배달의민족',  'FOOD'),
  (2, 12, '2026-08-11', 'EXPENSE',  12100, '이마트24',    'FOOD'),
  (2, 12, '2026-08-13', 'EXPENSE',  14600, '신전떡볶이',  'FOOD'),
  (2, 12, '2026-08-15', 'EXPENSE',   7400, 'CU',          'FOOD'),
  (2, 12, '2026-08-18', 'EXPENSE',  72200, '이마트',      'FOOD'),
  (2, 12, '2026-08-20', 'EXPENSE',  21900, '배달의민족',  'FOOD'),
  (2, 12, '2026-08-23', 'EXPENSE',  10800, '맘스터치',    'FOOD'),
  (2, 12, '2026-08-26', 'EXPENSE',  47300, '홈플러스',    'FOOD'),
  (2, 12, '2026-08-29', 'EXPENSE',  20600, '배달의민족',  'FOOD'),
  (2, 12, '2026-08-31', 'EXPENSE',   7900, 'GS25',        'FOOD');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-08-05', 'EXPENSE',   5300, '스타벅스',    'LEISURE_SHOPPING'),
  (2, 12, '2026-08-12', 'EXPENSE',  13500, '넷플릭스',    'LEISURE_SHOPPING'),
  (2, 12, '2026-08-17', 'EXPENSE',  14000, 'CGV',         'LEISURE_SHOPPING'),
  (2, 12, '2026-08-22', 'EXPENSE',  47600, '무신사',      'LEISURE_SHOPPING'),
  (2, 12, '2026-08-28', 'EXPENSE',  51000, '쿠팡',        'LEISURE_SHOPPING');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-08-03', 'EXPENSE',  11300, '다이소',      'LIVING_MEDICAL'),
  (2, 12, '2026-08-14', 'EXPENSE',  22800, '올리브영',    'LIVING_MEDICAL'),
  (2, 12, '2026-08-21', 'EXPENSE',  24600, '쿠팡',        'LIVING_MEDICAL'),
  (2, 12, '2026-08-30', 'EXPENSE',  13300, '다이소',      'LIVING_MEDICAL');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 11, '2026-09-01', 'EXPENSE', 138700, 'LH 임대료',         'HOUSING_UTILITY'),
  (2, 11, '2026-09-05', 'EXPENSE',  38900, '한국전력·도시가스',  'HOUSING_UTILITY'),
  (2, 11, '2026-09-05', 'EXPENSE',  29700, 'KT알뜰폰',          'HOUSING_UTILITY'),
  (2, 11, '2026-09-10', 'EXPENSE',  58000, '티머니 충전',        'TRANSPORT'),
  (2, 11, '2026-09-15', 'EXPENSE',  50000, '우리 정기적금',      'SAVINGS');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-09-02', 'EXPENSE',   8400, 'GS25',        'FOOD'),
  (2, 12, '2026-09-05', 'EXPENSE',  41600, '홈플러스',    'FOOD'),
  (2, 12, '2026-09-08', 'EXPENSE',   8700, '김밥천국',    'FOOD'),
  (2, 12, '2026-09-10', 'EXPENSE',  19300, '배달의민족',  'FOOD'),
  (2, 12, '2026-09-12', 'EXPENSE',  11800, '이마트24',    'FOOD'),
  (2, 12, '2026-09-14', 'EXPENSE',   8900, 'CU',          'FOOD');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-09-04', 'EXPENSE',   5700, '스타벅스',    'LEISURE_SHOPPING'),
  (2, 12, '2026-09-09', 'EXPENSE',  13500, '넷플릭스',    'LEISURE_SHOPPING'),
  (2, 12, '2026-09-13', 'EXPENSE',  17300, '무신사',      'LEISURE_SHOPPING');

INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 12, '2026-09-06', 'EXPENSE',   9200, '다이소',      'LIVING_MEDICAL'),
  (2, 12, '2026-09-11', 'EXPENSE',   9200, '올리브영',    'LIVING_MEDICAL');

-- 새 적금: 두 달 정상 납입. 9월 납입과 9월 카페 급여는 의도적으로 없음.
INSERT INTO seed_demo2_txn (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
  (2, 11, '2026-07-23', 'EXPENSE', 200000, 'KB국민 시연 정기적금', 'SAVINGS'),
  (2, 11, '2026-08-23', 'EXPENSE', 200000, 'KB국민 시연 정기적금', 'SAVINGS'),
  (2, 11, '2026-09-20', 'INCOME', 500000, '경기도청 자립수당', NULL);

-- 검산: 원본 주석 대신 실제 INSERT 행에서 다시 계산 (생활비 지출은 SAVINGS 제외).
-- 2026-07: 35건 / 수입 912,800 / 생활비 지출 823,600 / 적금 이체 350,000
-- 2026-08: 31건 / 수입 850,880 / 생활비 지출 776,300 / 적금 이체 300,000
-- 2026-09: 17건 / 수입 500,000 / 생활비 지출 418,900 / 적금 이체 50,000
-- 09-26 입출금 3,056,400 + 적금 3,580,000 = 총 6,636,400원.
-- 09-15 원본 총액 6,136,400 + 09-20 수당 500,000. 새 적금 400,000은 내부 이동.


-- 사전 조건: 실패하면 이 트랜잭션은 중단되어 어떤 데이터도 교체되지 않음.
DO $guard$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM member WHERE id = 2 AND email = 'demo2@fledge.dev') THEN
        RAISE EXCEPTION 'Expected demo2@fledge.dev with member id 2; no changes applied';
    END IF;
    IF EXISTS (
        SELECT 1 FROM account a JOIN seed_demo2_account s ON s.id = a.id
        WHERE a.member_id <> 2 OR a.bank_name <> s.bank_name OR a.account_type <> s.account_type
    ) THEN
        RAISE EXCEPTION 'Account id 11..15 belongs to another member or has different identity';
    END IF;
    IF EXISTS (
        SELECT 1 FROM money_schedule m JOIN seed_demo2_schedule s ON s.id = m.id
        WHERE m.member_id <> 2 OR m.name <> s.name OR m.type <> s.type OR m.direction <> s.direction
    ) THEN
        RAISE EXCEPTION 'Schedule id 201..207 collision; inspect before assigning different ids';
    END IF;
    IF EXISTS (
        SELECT 1 FROM money_schedule m JOIN seed_demo2_schedule s
          ON m.member_id = 2 AND (m.name = s.name OR m.match_keyword = s.match_keyword)
        WHERE m.id <> s.id
    ) THEN
        RAISE EXCEPTION 'Equivalent schedule exists under another id; reconcile explicitly first';
    END IF;
END;
$guard$;

-- 삭제 대상은 원본과 추가 거래의 완전 일치 행(중복 포함), 그리고 시연의 누락을 복구할 9월 거래.
CREATE TEMP TABLE seed_demo2_delete_txn ON COMMIT DROP AS
SELECT t.id
FROM transaction t
WHERE t.member_id = 2 AND (
    EXISTS (
        SELECT 1 FROM seed_demo2_txn s
        WHERE t.account_id = s.account_id AND t.txn_date = s.txn_date
          AND t.txn_type = s.txn_type AND t.amount = s.amount
          AND t.merchant_name IS NOT DISTINCT FROM s.merchant_name
          AND t.category IS NOT DISTINCT FROM s.category
    )
    OR (t.account_id = 11 AND t.txn_date >= DATE '2026-09-01' AND t.txn_date < DATE '2026-10-01'
        AND ((t.merchant_name = 'KB국민 시연 정기적금' AND t.txn_type = 'EXPENSE')
          OR (t.merchant_name = '카페모디 급여' AND t.txn_type = 'INCOME')))
);

DO $references$
BEGIN
    IF EXISTS (
        SELECT 1 FROM money_cycle c JOIN seed_demo2_delete_txn d ON d.id = c.matched_transaction_id
        WHERE c.member_id <> 2 OR NOT EXISTS (SELECT 1 FROM seed_demo2_schedule s WHERE s.id = c.schedule_id)
    ) THEN
        RAISE EXCEPTION 'An unmanaged cycle references a seed transaction; refusing to reset unrelated state';
    END IF;
END;
$references$;

-- cycle 삭제가 care_signal -> care_response / referral_request 를 FK CASCADE로 정리함.
-- transaction을 먼저 삭제하면 DONE 사이클의 matched_transaction_id 제약에 걸릴 수 있음.
DELETE FROM money_cycle c
USING seed_demo2_schedule s
WHERE c.member_id = 2 AND c.schedule_id = s.id;

DELETE FROM transaction t USING seed_demo2_delete_txn d WHERE t.id = d.id AND t.member_id = 2;

INSERT INTO account (id, member_id, bank_name, account_type, balance, balance_updated_at)
SELECT id, 2, bank_name, account_type, balance, TIMESTAMPTZ '2026-09-26 09:41:00+09'
FROM seed_demo2_account
ON CONFLICT (id) DO UPDATE SET
    balance = EXCLUDED.balance, balance_updated_at = EXCLUDED.balance_updated_at;

INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category)
SELECT member_id, account_id, txn_date, txn_type, amount, merchant_name, category FROM seed_demo2_txn;

INSERT INTO money_schedule (id, member_id, direction, type, name, expected_amount, expected_day, match_keyword, is_active)
SELECT id, 2, direction, type, name, expected_amount, expected_day, match_keyword, true FROM seed_demo2_schedule
ON CONFLICT (id) DO UPDATE SET
    expected_amount = EXCLUDED.expected_amount, expected_day = EXCLUDED.expected_day,
    match_keyword = EXCLUDED.match_keyword, is_active = true, updated_at = now();

-- money_cycle / care_signal / care_response / referral_request 는 INSERT하지 않음.
-- 모든 신규 사이클 및 상담 상태는 이후 애플리케이션이 생성함.

-- 검증 실패 시 COMMIT하지 않고 전체 롤백. 기존 관리 외 거래는 검산 범위에서 제외.
DO $verify$
BEGIN
    IF (SELECT count(*) FROM account a JOIN seed_demo2_account s ON a.id = s.id
        WHERE a.member_id = 2 AND a.balance = s.balance) <> 5 THEN
        RAISE EXCEPTION 'Expected five accounts with matching balances';
    END IF;
    IF (SELECT sum(balance) FROM account WHERE member_id = 2 AND id BETWEEN 11 AND 15) <> 6636400 THEN
        RAISE EXCEPTION 'Expected account balance total 6636400';
    END IF;
    IF (SELECT count(*) FROM money_schedule m JOIN seed_demo2_schedule s ON s.id = m.id
        WHERE m.member_id = 2 AND m.is_active) <> 7 THEN
        RAISE EXCEPTION 'Expected seven active managed schedules';
    END IF;
    IF EXISTS (
        SELECT 1 FROM seed_demo2_txn s
        WHERE (SELECT count(*) FROM transaction t
               WHERE t.member_id = 2 AND t.account_id = s.account_id AND t.txn_date = s.txn_date
                 AND t.txn_type = s.txn_type AND t.amount = s.amount
                 AND t.merchant_name IS NOT DISTINCT FROM s.merchant_name
                 AND t.category IS NOT DISTINCT FROM s.category) <> 1
    ) THEN
        RAISE EXCEPTION 'Missing or duplicate seeded transaction';
    END IF;
    IF EXISTS (
        SELECT 1 FROM transaction WHERE member_id = 2 AND account_id = 11
          AND txn_date >= DATE '2026-09-01' AND txn_date < DATE '2026-10-01'
          AND ((merchant_name = 'KB국민 시연 정기적금' AND txn_type = 'EXPENSE')
            OR (merchant_name = '카페모디 급여' AND txn_type = 'INCOME'))
    ) THEN
        RAISE EXCEPTION 'September demo savings/salary must remain missing';
    END IF;
    IF EXISTS (SELECT 1 FROM money_cycle c JOIN seed_demo2_schedule s ON s.id = c.schedule_id WHERE c.member_id = 2) THEN
        RAISE EXCEPTION 'Managed cycles must be empty before application detection';
    END IF;
END;
$verify$;

-- 조회용 예상 결과: 현재 DB의 riskScore를 저장/변경하지 않음.
-- 9월 시나리오만 검산. 실제 탐지 구현에서도 회원·방향·기준일을 함께 확인해야 함.
WITH dates(as_of, expected_score) AS (
    VALUES (DATE '2026-09-23', 0), (DATE '2026-09-24', 25), (DATE '2026-09-26', 65)
), state AS (
    SELECT d.as_of, d.expected_score, s.id, s.name, s.type,
           CASE WHEN EXISTS (
               SELECT 1 FROM transaction t WHERE t.member_id = 2
                 AND t.txn_date BETWEEN DATE '2026-09-01' AND d.as_of
                 AND t.merchant_name = s.match_keyword
                 AND t.txn_type = CASE WHEN s.direction = 'OUT' THEN 'EXPENSE' ELSE 'INCOME' END
                 AND (s.direction = 'IN' OR t.amount = s.expected_amount)
           ) THEN 'DONE'
           WHEN d.as_of > make_date(2026, 9, s.expected_day) THEN 'MISSED'
           ELSE 'PENDING' END AS projected_status
    FROM dates d CROSS JOIN seed_demo2_schedule s
), scores AS (
    SELECT as_of, expected_score,
           LEAST(100,
             COALESCE(MAX(CASE WHEN type = 'SAVINGS' AND projected_status = 'MISSED' THEN 25 END), 0)
             + COALESCE(MAX(CASE WHEN type IN ('PART_TIME', 'OTHER_REGULAR', 'SALARY') AND projected_status = 'MISSED' THEN 40 END), 0)
             + COALESCE(MAX(CASE WHEN projected_status = 'MISSED' THEN
                 CASE type WHEN 'RENT' THEN 40 WHEN 'UTILITY' THEN 35 WHEN 'TELECOM' THEN 30 END END), 0)
           ) AS projected_risk_score,
           string_agg(name || ': ' || projected_status, ', ' ORDER BY id) AS schedule_states
    FROM state GROUP BY as_of, expected_score
)
SELECT *, projected_risk_score = expected_score AS passed FROM scores ORDER BY as_of;

-- 재실행 확인: 아래 월별 count/sum이 동일해야 함(자동 생성 PK는 달라질 수 있음).
SELECT date_trunc('month', txn_date)::date AS month, count(*) AS transaction_count,
       sum(amount) FILTER (WHERE txn_type = 'INCOME') AS income,
       sum(amount) FILTER (WHERE txn_type = 'EXPENSE' AND category IS DISTINCT FROM 'SAVINGS') AS living_expense,
       sum(amount) FILTER (WHERE txn_type = 'EXPENSE' AND category = 'SAVINGS') AS savings_transfer
FROM seed_demo2_txn GROUP BY 1 ORDER BY 1;

-- 명시 ID를 넣었으므로 다음 자동 발급 값이 충돌하지 않게 조정. 기존 시퀀스를 뒤로 돌리지 않음.
-- PostgreSQL 시퀀스 증가는 롤백되지 않으며 검증 이후 마지막에 수행함.
SELECT setval(pg_get_serial_sequence('account', 'id'),
              GREATEST((SELECT max(id) FROM account), nextval(pg_get_serial_sequence('account', 'id'))), true);
SELECT setval(pg_get_serial_sequence('money_schedule', 'id'),
              GREATEST((SELECT max(id) FROM money_schedule), nextval(pg_get_serial_sequence('money_schedule', 'id'))), true);

COMMIT;
