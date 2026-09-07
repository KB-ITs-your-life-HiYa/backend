-- 로컬 개발·데모용 회원 계정.
--
-- 【규칙】
--   이 파일은 로컬 프로필에만 적용된다. Supabase 에는 들어가지 않는다.
--   R__ 는 내용이 바뀌면 자동으로 다시 적용되므로, 여러 번 실행돼도 결과가 같아야 한다.
--
-- 【DELETE 를 쓰지 않는 이유】
--   member 를 통째로 지우면 habit_puzzle_progress / habit_quiz_answer 처럼
--   ON DELETE CASCADE 가 아닌 자식 테이블 FK 에 막혀 기동이 실패한다.
--   데모 계정만 upsert 로 맞추면 다른 도메인 시드·로컬 진행 데이터를 보존한다.
--
-- 【시드 파일 나누기】
--   도메인별로 파일을 나눈다. 한 파일에 여럿이 쓰면 충돌한다.
--     R__seed_01_member.sql    회원          <- 이 파일
--     R__seed_02_housing.sql   주거
--     R__seed_03_budget.sql    계좌·거래·정기납부
--     R__seed_04_subsidy.sql   지원금
--     R__seed_05_care.sql      케어 신호
--   Flyway 는 파일명 순서대로 적용한다. member 가 먼저여야 나머지가 FK 를 걸 수 있으므로 01 이다.
--
-- 【비밀번호】
--   demo1/demo2 는 demo1234, counselor 계정은 counselor1234.
--   BCrypt($2a$, 10 rounds) 해시로 저장한다. Spring Security 기본 형식이다.
--
-- 【날짜를 상대값으로 두는 이유】
--   보호종료일을 고정 날짜로 넣으면 시간이 지나면서 D-day 가 어긋난다.
--   CURRENT_DATE 기준으로 두면 언제 실행해도 의도한 시점이 유지된다.
--   D-1825 = 보호종료일 + 1825일 (자립수당 5년)
--
-- 【상담사 계정】
--   counselor@fledge.local — 상담사(담당자) 포털 로그인용. role='COUNSELOR' 라
--   프론트가 로그인 직후 자립청년 앱 대신 상담사 포털로 보낸다.
--   birth_date/gender/region_code/protection_status 등은 자립청년 전용 필드라
--   상담사에게는 의미가 없지만, 컬럼이 NOT NULL 이라 스키마를 만족시키는 값만 채운다.

-- region_sigungu_code 는 sigungu 테이블의 코드다. 이름으로 직접 안 쓰는 이유는
-- '중구'처럼 여러 시/도에 중복되는 이름이 있어서다(#19).
--   41115 = 경기도 수원시팔달구,  28177 = 인천광역시 미추홀구
INSERT INTO member (
    id, email, password_hash, role, birth_date, gender,
    region_code, region_sigungu_code,
    protection_status, protection_end_date, protection_type, home_region_code,
    created_at
) VALUES
-- 계정 1 — 자립 초기. 앱을 4개월쯤 쓴 사용자
--   보호종료 550일 경과 → D-1825 까지 1275일 남음
(1, 'demo1@fledge.dev', '$2a$10$4hz9VjP/e3kYPu9PFnSwr.chUFEJ6lF/oJeg.nASGLQxbiqYEAMD6', 'YOUTH',
 CURRENT_DATE - INTERVAL '20 years', 'FEMALE',
 '41', '41115',
 'ENDED', CURRENT_DATE - 550, 'FACILITY', '41',
 now() - INTERVAL '4 months'),

-- 계정 2 — D-365 대비모드 안에서 절반쯤 온 시점
--   보호종료 1645일 경과 → D-1825 까지 180일 남음
--   자립수당이 6개월 뒤 끊긴다. 가장 도움이 절실한 구간이다.
--   자립준비청년 5년 이내 요건은 아직 충족하지만(1645 < 1825),
--   6개월 뒤에는 전용 임대주택 1순위 자격이 만료된다.
--   다만 만 39세 이하라 청년 트랙으로는 계속 신청할 수 있다(시세 40% -> 50%).
--
--   home_region_code 를 경기(41)로 두어 자립정착금 1,500만원 기준을 맞춘다.
--   지금은 인천(28)에 살아서, 정착금은 경기 기준 / 임대주택 공고는 인천 기준으로 갈린다.
(2, 'demo2@fledge.dev', '$2a$10$4hz9VjP/e3kYPu9PFnSwr.chUFEJ6lF/oJeg.nASGLQxbiqYEAMD6', 'YOUTH',
 CURRENT_DATE - INTERVAL '22 years', 'MALE',
 '28', '28177',
 'ENDED', CURRENT_DATE - 1645, 'FOSTER_CARE', '41',
 now() - INTERVAL '1 year'),

-- 계정 3 — 상담사(담당자). 자립청년 데이터가 아니라 스키마 제약을 만족시키는
-- 자리채움 값만 넣는다. protection_status 를 IN_CARE 로 두면 protection_end_date
-- 없이도 CHECK 제약(ck_member_end_date_required)을 만족한다.
(3, 'counselor@fledge.local', '$2a$10$/m0eVg84FQdzDMpMAnO7eeDwkPKFRxxEwuMqfAlYYVJZ.cMAU.mae', 'COUNSELOR',
 CURRENT_DATE - INTERVAL '35 years', 'FEMALE',
 '11', NULL,
 'IN_CARE', NULL, NULL, NULL,
 now() - INTERVAL '2 years')
ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    birth_date = EXCLUDED.birth_date,
    gender = EXCLUDED.gender,
    region_code = EXCLUDED.region_code,
    region_sigungu_code = EXCLUDED.region_sigungu_code,
    protection_status = EXCLUDED.protection_status,
    protection_end_date = EXCLUDED.protection_end_date,
    protection_type = EXCLUDED.protection_type,
    home_region_code = EXCLUDED.home_region_code,
    created_at = EXCLUDED.created_at;

-- 시퀀스를 뒤로 밀어둔다. 이후 가입하는 회원이 1, 2, 3 과 충돌하지 않도록.
SELECT setval(
    'member_id_seq',
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM member), 1000)
);
