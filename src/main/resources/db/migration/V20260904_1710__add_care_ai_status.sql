-- 자유입력은 먼저 저장하고 Gemini 호출 결과를 나중에 반영한다.
ALTER TABLE care_response ADD COLUMN ai_status VARCHAR(20);
ALTER TABLE care_response ADD CONSTRAINT ck_care_response_ai_status
    CHECK (ai_status IS NULL OR ai_status IN ('PENDING', 'READY', 'ERROR'));
