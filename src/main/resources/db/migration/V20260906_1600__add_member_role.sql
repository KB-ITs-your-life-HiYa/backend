-- 로그인 계정 종류(자립청년/상담사)를 구분하기 위한 컬럼.
--
-- 지금까지 member 는 전부 자립준비청년 본인 계정이었다. 이번에 상담사(담당자)가
-- PC 웹으로 접속하는 포털을 추가하면서, 같은 /auth/login 을 그대로 쓰되 이 컬럼으로
-- 로그인 후 어떤 화면(청년용 앱 / 상담사 포털)을 보여줄지 프론트에서 나눈다.
--
-- 기존 회원은 전부 자립준비청년이므로 DEFAULT 'YOUTH' 로 두면 백필이 필요 없다.
ALTER TABLE member ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'YOUTH';

ALTER TABLE member ADD CONSTRAINT ck_member_role
    CHECK (role IN ('YOUTH', 'COUNSELOR'));

COMMENT ON COLUMN member.role IS '로그인 계정 종류. YOUTH=자립준비청년 본인, COUNSELOR=담당 상담사';
