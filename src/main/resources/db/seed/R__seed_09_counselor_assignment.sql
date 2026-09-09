-- 상담사 포털에서 표시할 데모 회원의 기본 연락처.
-- 사용자가 수정한 값은 반복 시드가 다시 실행되어도 덮어쓰지 않는다.
UPDATE member m
SET name = COALESCE(m.name, seed.name),
    phone = COALESCE(m.phone, seed.phone)
FROM (VALUES
    ('demo1@fledge.dev', '김도윤', '010-2841-7723'),
    ('demo2@fledge.dev', '이서윤', '010-9034-1182'),
    ('counselor@fledge.local', '김민지', '010-3150-1825')
) AS seed(email, name, phone)
WHERE m.email = seed.email;

-- 로그인 계정(member)과 기존 상담사 업무 프로필(counselor)을 연결한다.
INSERT INTO counselor (
    member_id, email, password_hash, name, phone,
    organization, region_code, is_active, created_at)
SELECT m.id, m.email, m.password_hash, m.name, m.phone,
       '자립동행 지원센터', m.region_code, true, m.created_at
FROM member m
WHERE m.email = 'counselor@fledge.local'
  AND m.role = 'COUNSELOR'
ON CONFLICT (email) DO UPDATE SET
    member_id = EXCLUDED.member_id,
    password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    organization = EXCLUDED.organization,
    region_code = EXCLUDED.region_code,
    is_active = EXCLUDED.is_active;

-- 데모 상담사에게 기존 데모 청년 두 명이 이미 배정된 상태로 시작한다.
INSERT INTO counselor_youth_assignment (counselor_id, youth_member_id, assigned_at)
SELECT c.id, youth.id, now()
FROM counselor c
JOIN member counselor_member ON counselor_member.id = c.member_id
CROSS JOIN member youth
WHERE counselor_member.email = 'counselor@fledge.local'
  AND counselor_member.role = 'COUNSELOR'
  AND youth.email IN ('demo1@fledge.dev', 'demo2@fledge.dev')
  AND youth.role = 'YOUTH'
  AND NOT EXISTS (
      SELECT 1
      FROM counselor_youth_assignment existing
      WHERE existing.youth_member_id = youth.id
        AND existing.unassigned_at IS NULL
  );
