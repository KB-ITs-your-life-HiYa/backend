-- 데모 영상에서는 주거 자격 정보를 한 번 입력한 상태에서 시작한다.
-- 사용자가 이후 수정한 값은 Flyway 재실행 시에도 덮어쓰지 않는다.
INSERT INTO housing_eligibility_profile (member_id, is_homeless, is_married, updated_at)
SELECT id, true, false, now()
FROM member
WHERE email IN ('demo1@fledge.dev', 'demo2@fledge.dev')
ON CONFLICT (member_id) DO NOTHING;
