-- demo1 회원의 금융 시드 데이터 (계좌, 거래 내역).
--
-- 【규칙】
--   이 파일은 로컬 프로필에만 적용된다. Supabase 에는 들어가지 않는다.
--   R__ 는 내용이 바뀌면 자동으로 다시 적용되므로, 여러 번 실행돼도 결과가 같아야 한다.
--
-- 【의존성】
--   account, transaction 모두 member 를 FK 로 참조하므로 R__seed_01_member.sql 이후에 적용돼야 한다.
--   Flyway 는 파일명 순서대로 적용하므로 01 -> 02 -> 03 순서가 보장된다.
--
-- 【날짜를 상대값으로 두는 이유】
--   고정 날짜로 넣으면 시간이 지나면서 최근 거래 내역이 과거로 밀려난다.
--   CURRENT_DATE 기준으로 두면 언제 실행해도 최근 활동으로 보인다.


-- ---------------------------------------------------------------------
-- account : demo1(member_id=1) 잔액 기준 2026-09-15
--   입출금 6,873,400 + 적금 900,000 = 순자산 7,773,400
-- ---------------------------------------------------------------------
INSERT INTO account (id, member_id, bank_name, account_type, balance, balance_updated_at) VALUES
                                                                                              (1, 1, 'KB국민 주거래 통장', 'DEPOSIT', 5487200, '2026-09-15 09:41:00+09'),
                                                                                              (2, 1, '신한 SOL 입출금',   'DEPOSIT', 1386200, '2026-09-15 09:41:00+09'),
                                                                                              (3, 1, 'KB국민 자유적금', 'SAVINGS',  900000, '2026-09-15 09:41:00+09');

SELECT setval('account_id_seq', 100, true);

-- ---------------------------------------------------------------------
-- transaction : 2026-07-01 ~ 2026-09-15
--   account 1 = 고정비·수당 입금·적금 이체 / account 2 = 생활비 카드 결제
--   category = 'SAVINGS' 는 지출 집계에서 제외
-- ---------------------------------------------------------------------

-- 2026년 7월
INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
                                                                                                         (1, 1, '2026-07-01', 'EXPENSE', 151300, 'LH 임대료',          'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-07-02', 'EXPENSE',   8400, 'GS25',               'FOOD'),
                                                                                                         (1, 2, '2026-07-04', 'EXPENSE',  19500, '배달의민족',          'FOOD'),
                                                                                                         (1, 1, '2026-07-05', 'EXPENSE',  47800, '한국전력·도시가스',   'HOUSING_UTILITY'),
                                                                                                         (1, 1, '2026-07-05', 'EXPENSE',  33000, 'KT알뜰폰',           'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-07-05', 'EXPENSE',   5600, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-07-06', 'EXPENSE',  47800, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-07-07', 'EXPENSE',  11800, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-07-08', 'EXPENSE',   9000, '김밥천국',            'FOOD'),
                                                                                                         (1, 2, '2026-07-09', 'EXPENSE',  17900, '쿠팡',                'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-07-10', 'EXPENSE',  90000, '티머니 충전',         'TRANSPORT'),
                                                                                                         (1, 2, '2026-07-10', 'EXPENSE',  12300, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-07-11', 'EXPENSE',  15000, 'CGV',                 'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-07-12', 'EXPENSE',  23900, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-07-13', 'EXPENSE',  79800, '무신사',              'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-07-14', 'EXPENSE',  13000, '한촌설렁탕',          'FOOD'),
                                                                                                         (1, 1, '2026-07-15', 'EXPENSE',  50000, 'KB국민 자유적금',     'SAVINGS'),
                                                                                                         (1, 2, '2026-07-16', 'EXPENSE',   7600, 'CU',                  'FOOD'),
                                                                                                         (1, 2, '2026-07-17', 'EXPENSE',  22400, '교보문고',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-07-18', 'EXPENSE',  72500, '이마트',              'FOOD'),
                                                                                                         (1, 2, '2026-07-19', 'EXPENSE',  26300, '올리브영',            'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-07-20', 'INCOME',  500000, '경기도청 자립수당',    NULL),
                                                                                                         (1, 2, '2026-07-20', 'EXPENSE',  21400, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-07-21', 'EXPENSE',  13500, '넷플릭스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-07-22', 'EXPENSE',  10900, '맘스터치',            'FOOD'),
                                                                                                         (1, 2, '2026-07-23', 'EXPENSE',  13500, '수원연세의원',        'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-07-23', 'EXPENSE',   4800, '팔달약국',            'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-07-24', 'EXPENSE',  52300, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-07-25', 'EXPENSE',   6100, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-07-26', 'EXPENSE',  17500, '신전떡볶이',          'FOOD'),
                                                                                                         (1, 2, '2026-07-27', 'EXPENSE',  76000, '쿠팡',                'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-07-28', 'EXPENSE',   9800, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-07-29', 'EXPENSE',  26200, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-07-30', 'EXPENSE',  45900, '이마트',              'FOOD'),
                                                                                                         (1, 2, '2026-07-31', 'EXPENSE',  14600, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-07-31', 'EXPENSE',   5000, '다이소',              'LIVING_MEDICAL');

-- 2026년 8월
INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
                                                                                                         (1, 1, '2026-08-01', 'EXPENSE', 151300, 'LH 임대료',          'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-08-03', 'EXPENSE',   7900, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-08-04', 'EXPENSE',  10700, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-08-05', 'EXPENSE',  52400, '한국전력·도시가스',   'HOUSING_UTILITY'),
                                                                                                         (1, 1, '2026-08-05', 'EXPENSE',  33000, 'KT알뜰폰',           'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-08-05', 'EXPENSE',  43600, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-08-06', 'EXPENSE',   5600, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-08-07', 'EXPENSE',   8500, '김밥천국',            'FOOD'),
                                                                                                         (1, 2, '2026-08-09', 'EXPENSE',  20800, '배달의민족',          'FOOD'),
                                                                                                         (1, 1, '2026-08-10', 'EXPENSE',  80000, '티머니 충전',         'TRANSPORT'),
                                                                                                         (1, 2, '2026-08-11', 'EXPENSE',  11400, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-08-12', 'EXPENSE',  13500, '넷플릭스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-08-13', 'EXPENSE',  13200, '신전떡볶이',          'FOOD'),
                                                                                                         (1, 2, '2026-08-14', 'EXPENSE',  23400, '올리브영',            'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-08-15', 'EXPENSE',  50000, 'KB국민 자유적금',     'SAVINGS'),
                                                                                                         (1, 2, '2026-08-15', 'EXPENSE',   7100, 'CU',                  'FOOD'),
                                                                                                         (1, 2, '2026-08-16', 'EXPENSE',  15000, 'CGV',                 'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-08-17', 'EXPENSE',  59600, '이마트',              'FOOD'),
                                                                                                         (1, 2, '2026-08-19', 'EXPENSE',  22600, '배달의민족',          'FOOD'),
                                                                                                         (1, 1, '2026-08-20', 'INCOME',  500000, '경기도청 자립수당',    NULL),
                                                                                                         (1, 2, '2026-08-20', 'EXPENSE',  43800, '무신사',              'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-08-21', 'EXPENSE',  16900, '쿠팡',                'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-08-22', 'EXPENSE',  10400, '맘스터치',            'FOOD'),
                                                                                                         (1, 2, '2026-08-25', 'EXPENSE',  48700, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-08-27', 'EXPENSE',  50800, '쿠팡',                'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-08-28', 'EXPENSE',  21300, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-08-29', 'EXPENSE',  13900, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-08-30', 'EXPENSE',   6200, 'GS25',                'FOOD');

-- 2026년 9월 (1~15일, 자립수당 20일 입금분은 미반영)
INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
                                                                                                         (1, 1, '2026-09-01', 'EXPENSE', 151300, 'LH 임대료',          'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-09-02', 'EXPENSE',   8800, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-09-03', 'EXPENSE',   8700, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-09-04', 'EXPENSE',  21700, '배달의민족',          'FOOD'),
                                                                                                         (1, 1, '2026-09-05', 'EXPENSE',  41600, '한국전력·도시가스',   'HOUSING_UTILITY'),
                                                                                                         (1, 1, '2026-09-05', 'EXPENSE',  33000, 'KT알뜰폰',           'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-09-05', 'EXPENSE',   6100, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-09-06', 'EXPENSE',  42300, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-09-08', 'EXPENSE',   9000, '김밥천국',            'FOOD'),
                                                                                                         (1, 2, '2026-09-09', 'EXPENSE',  13500, '넷플릭스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 1, '2026-09-10', 'EXPENSE',  80000, '티머니 충전',         'TRANSPORT'),
                                                                                                         (1, 2, '2026-09-10', 'EXPENSE',  23500, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-09-11', 'EXPENSE',  13300, '올리브영',            'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-09-12', 'EXPENSE',  11900, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-09-13', 'EXPENSE',  35600, '무신사',              'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-09-14', 'EXPENSE',  16400, '신전떡볶이',          'FOOD'),
                                                                                                         (1, 1, '2026-09-15', 'EXPENSE',  50000, 'KB국민 자유적금',     'SAVINGS'),
                                                                                                         (1, 2, '2026-09-15', 'EXPENSE',   8000, 'CU',                  'FOOD');