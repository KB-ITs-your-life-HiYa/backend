-- 상담 응답과 정책 조회를 별도 트랜잭션으로 처리한다. 성공한 카드는 재접속 시 그대로 복원한다.
ALTER TABLE care_response ADD COLUMN policy_status VARCHAR(20);
ALTER TABLE care_response ADD COLUMN policy_cards TEXT;
ALTER TABLE care_response ADD CONSTRAINT ck_care_response_policy_status
    CHECK (policy_status IS NULL OR policy_status IN ('PENDING', 'READY', 'EMPTY', 'ERROR'));
