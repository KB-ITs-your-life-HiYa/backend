-- 데모 영상에서는 주거 자격 정보를 한 번 입력한 상태에서 시작한다.
-- 사용자가 이후 수정한 값은 Flyway 재실행 시에도 덮어쓰지 않는다.
INSERT INTO housing_eligibility_profile (
    member_id, is_homeless, is_married, youth_purchase_priority_basis, updated_at)
SELECT id, true, false,
       CASE WHEN email = 'demo2@fledge.dev' THEN 'BENEFIT_RECIPIENT' ELSE NULL END,
       now()
FROM member
WHERE email IN ('demo1@fledge.dev', 'demo2@fledge.dev')
ON CONFLICT (member_id) DO NOTHING;

-- demo2는 실제 청년 매입임대 1순위 시연에 사용한다.
-- NONE을 포함해 사용자가 직접 선택한 값은 덮어쓰지 않는다.
UPDATE housing_eligibility_profile p
SET youth_purchase_priority_basis = 'BENEFIT_RECIPIENT', updated_at = now()
FROM member m
WHERE p.member_id = m.id
  AND m.email = 'demo2@fledge.dev'
  AND p.youth_purchase_priority_basis IS NULL;
