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

-- 2026년 3월
INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
                                                                                                         (1, 1, '2026-03-01', 'EXPENSE', 151300, 'LH 임대료',          'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-03-02', 'EXPENSE',   7300, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-03-04', 'EXPENSE',  22400, '배달의민족',          'FOOD'),
                                                                                                         (1, 1, '2026-03-05', 'EXPENSE',  58200, '한국전력·도시가스',   'HOUSING_UTILITY'),
                                                                                                         (1, 1, '2026-03-05', 'EXPENSE',  33000, 'KT알뜰폰',           'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-03-05', 'EXPENSE',   5600, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-03-06', 'EXPENSE',  51200, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-03-07', 'EXPENSE',   9800, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-03-08', 'EXPENSE',   8500, '김밥천국',            'FOOD'),
                                                                                                         (1, 2, '2026-03-09', 'EXPENSE',  13500, '넷플릭스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 1, '2026-03-10', 'EXPENSE',  90000, '티머니 충전',         'TRANSPORT'),
                                                                                                         (1, 2, '2026-03-11', 'EXPENSE',  10600, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-03-12', 'EXPENSE',  15000, 'CGV',                 'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-03-13', 'EXPENSE',  19800, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-03-14', 'EXPENSE',  21400, '올리브영',            'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-03-15', 'EXPENSE',  50000, 'KB국민 자유적금',     'SAVINGS'),
                                                                                                         (1, 2, '2026-03-15', 'EXPENSE',   6900, 'CU',                  'FOOD'),
                                                                                                         (1, 2, '2026-03-16', 'EXPENSE',  58900, '무신사',              'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-03-17', 'EXPENSE',  63400, '이마트',              'FOOD'),
                                                                                                         (1, 2, '2026-03-18', 'EXPENSE',  15600, '쿠팡',                'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-03-19', 'EXPENSE',  11200, '맘스터치',            'FOOD'),
                                                                                                         (1, 1, '2026-03-20', 'INCOME',  500000, '경기도청 자립수당',    NULL),
                                                                                                         (1, 2, '2026-03-21', 'EXPENSE',  24700, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-03-22', 'EXPENSE',  18700, '교보문고',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-03-23', 'EXPENSE',  15300, '신전떡볶이',          'FOOD'),
                                                                                                         (1, 2, '2026-03-24', 'EXPENSE',  12000, '수원연세의원',        'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-03-24', 'EXPENSE',   5200, '팔달약국',            'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-03-25', 'EXPENSE',  46800, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-03-26', 'EXPENSE',  42300, '쿠팡',                'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-03-27', 'EXPENSE',   8900, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-03-28', 'EXPENSE',   7600, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-03-29', 'EXPENSE',  20600, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-03-30', 'EXPENSE',   6100, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-03-31', 'EXPENSE',  13100, '이마트24',            'FOOD');

-- 2026년 4월
INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
                                                                                                         (1, 1, '2026-04-01', 'EXPENSE', 151300, 'LH 임대료',          'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-04-02', 'EXPENSE',   8200, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-04-03', 'EXPENSE',  18900, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-04-04', 'EXPENSE',  11200, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-04-05', 'EXPENSE',  38900, '한국전력·도시가스',   'HOUSING_UTILITY'),
                                                                                                         (1, 1, '2026-04-05', 'EXPENSE',  33000, 'KT알뜰폰',           'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-04-05', 'EXPENSE',   6100, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-04-06', 'EXPENSE',  44700, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-04-08', 'EXPENSE',   9000, '김밥천국',            'FOOD'),
                                                                                                         (1, 2, '2026-04-09', 'EXPENSE',  13500, '넷플릭스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 1, '2026-04-10', 'EXPENSE',  80000, '티머니 충전',         'TRANSPORT'),
                                                                                                         (1, 2, '2026-04-10', 'EXPENSE',  11800, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-04-11', 'EXPENSE',  19700, '올리브영',            'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-04-12', 'EXPENSE',  21300, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-04-13', 'EXPENSE',  47200, '무신사',              'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-04-14', 'EXPENSE',  13000, '한촌설렁탕',          'FOOD'),
                                                                                                         (1, 1, '2026-04-15', 'EXPENSE',  50000, 'KB국민 자유적금',     'SAVINGS'),
                                                                                                         (1, 2, '2026-04-16', 'EXPENSE',   7400, 'CU',                  'FOOD'),
                                                                                                         (1, 2, '2026-04-17', 'EXPENSE',  15000, 'CGV',                 'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-04-18', 'EXPENSE',  57600, '이마트',              'FOOD'),
                                                                                                         (1, 2, '2026-04-19', 'EXPENSE',  14300, '쿠팡',                'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-04-20', 'INCOME',  500000, '경기도청 자립수당',    NULL),
                                                                                                         (1, 2, '2026-04-20', 'EXPENSE',  23100, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-04-21', 'EXPENSE',  24800, '교보문고',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-04-22', 'EXPENSE',  10400, '맘스터치',            'FOOD'),
                                                                                                         (1, 2, '2026-04-24', 'EXPENSE',  49200, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-04-25', 'EXPENSE',   8400, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-04-26', 'EXPENSE',  16800, '신전떡볶이',          'FOOD'),
                                                                                                         (1, 2, '2026-04-27', 'EXPENSE',  51300, '쿠팡',                'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-04-28', 'EXPENSE',   9100, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-04-30', 'EXPENSE',  12600, '이마트24',            'FOOD');

-- 2026년 5월
INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
                                                                                                         (1, 1, '2026-05-01', 'EXPENSE', 151300, 'LH 임대료',          'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-05-02', 'EXPENSE',   7800, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-05-03', 'EXPENSE',  10300, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-05-04', 'EXPENSE',  20500, '배달의민족',          'FOOD'),
                                                                                                         (1, 1, '2026-05-05', 'EXPENSE',  35400, '한국전력·도시가스',   'HOUSING_UTILITY'),
                                                                                                         (1, 1, '2026-05-05', 'EXPENSE',  33000, 'KT알뜰폰',           'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-05-05', 'EXPENSE',   5600, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-05-06', 'EXPENSE',  47300, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-05-08', 'EXPENSE',  15000, 'CGV',                 'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-05-09', 'EXPENSE',   8500, '김밥천국',            'FOOD'),
                                                                                                         (1, 1, '2026-05-10', 'EXPENSE',  80000, '티머니 충전',         'TRANSPORT'),
                                                                                                         (1, 2, '2026-05-11', 'EXPENSE',  12200, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-05-12', 'EXPENSE',  13500, '넷플릭스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-05-13', 'EXPENSE',  22800, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-05-14', 'EXPENSE',  24600, '올리브영',            'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-05-15', 'EXPENSE',  50000, 'KB국민 자유적금',     'SAVINGS'),
                                                                                                         (1, 2, '2026-05-15', 'EXPENSE',   7100, 'CU',                  'FOOD'),
                                                                                                         (1, 2, '2026-05-16', 'EXPENSE',  63500, '무신사',              'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-05-17', 'EXPENSE',  61200, '이마트',              'FOOD'),
                                                                                                         (1, 2, '2026-05-18', 'EXPENSE',  16800, '쿠팡',                'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-05-19', 'EXPENSE',  11600, '맘스터치',            'FOOD'),
                                                                                                         (1, 1, '2026-05-20', 'INCOME',  500000, '경기도청 자립수당',    NULL),
                                                                                                         (1, 2, '2026-05-21', 'EXPENSE',  19400, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-05-22', 'EXPENSE',  21300, '교보문고',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-05-23', 'EXPENSE',  14700, '신전떡볶이',          'FOOD'),
                                                                                                         (1, 2, '2026-05-24', 'EXPENSE',  13500, '수원연세의원',        'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-05-24', 'EXPENSE',   4800, '팔달약국',            'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-05-25', 'EXPENSE',  44100, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-05-26', 'EXPENSE',  38700, '쿠팡',                'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-05-27', 'EXPENSE',   8300, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-05-28', 'EXPENSE',   6900, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-05-29', 'EXPENSE',  25600, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-05-30', 'EXPENSE',   6100, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-05-31', 'EXPENSE',  11500, '이마트24',            'FOOD');

-- 2026년 6월
INSERT INTO transaction (member_id, account_id, txn_date, txn_type, amount, merchant_name, category) VALUES
                                                                                                         (1, 1, '2026-06-01', 'EXPENSE', 151300, 'LH 임대료',          'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-06-02', 'EXPENSE',   8600, 'GS25',                'FOOD'),
                                                                                                         (1, 2, '2026-06-03', 'EXPENSE',  12700, '다이소',              'LIVING_MEDICAL'),
                                                                                                         (1, 2, '2026-06-04', 'EXPENSE',  23200, '배달의민족',          'FOOD'),
                                                                                                         (1, 1, '2026-06-05', 'EXPENSE',  44300, '한국전력·도시가스',   'HOUSING_UTILITY'),
                                                                                                         (1, 1, '2026-06-05', 'EXPENSE',  33000, 'KT알뜰폰',           'HOUSING_UTILITY'),
                                                                                                         (1, 2, '2026-06-05', 'EXPENSE',   6100, '스타벅스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-06-06', 'EXPENSE',  45900, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-06-08', 'EXPENSE',   9000, '김밥천국',            'FOOD'),
                                                                                                         (1, 2, '2026-06-09', 'EXPENSE',  22800, '올리브영',            'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-06-10', 'EXPENSE',  90000, '티머니 충전',         'TRANSPORT'),
                                                                                                         (1, 2, '2026-06-10', 'EXPENSE',  13400, '이마트24',            'FOOD'),
                                                                                                         (1, 2, '2026-06-11', 'EXPENSE',  13500, '넷플릭스',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-06-12', 'EXPENSE',  21700, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-06-13', 'EXPENSE',  71400, '무신사',              'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-06-14', 'EXPENSE',  13000, '한촌설렁탕',          'FOOD'),
                                                                                                         (1, 1, '2026-06-15', 'EXPENSE',  50000, 'KB국민 자유적금',     'SAVINGS'),
                                                                                                         (1, 2, '2026-06-16', 'EXPENSE',   7600, 'CU',                  'FOOD'),
                                                                                                         (1, 2, '2026-06-17', 'EXPENSE',  15000, 'CGV',                 'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-06-18', 'EXPENSE',  68300, '이마트',              'FOOD'),
                                                                                                         (1, 2, '2026-06-19', 'EXPENSE',  18400, '쿠팡',                'LIVING_MEDICAL'),
                                                                                                         (1, 1, '2026-06-20', 'INCOME',  500000, '경기도청 자립수당',    NULL),
                                                                                                         (1, 2, '2026-06-20', 'EXPENSE',  22400, '배달의민족',          'FOOD'),
                                                                                                         (1, 2, '2026-06-21', 'EXPENSE',  19600, '교보문고',            'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-06-22', 'EXPENSE',  10900, '맘스터치',            'FOOD'),
                                                                                                         (1, 2, '2026-06-24', 'EXPENSE',  50600, '홈플러스',            'FOOD'),
                                                                                                         (1, 2, '2026-06-25', 'EXPENSE',  58200, '쿠팡',                'LEISURE_SHOPPING'),
                                                                                                         (1, 2, '2026-06-26', 'EXPENSE',  17200, '신전떡볶이',          'FOOD'),
                                                                                                         (1, 2, '2026-06-27', 'EXPENSE',   9200, '다이소',              'LIVING_MEDICAL');

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