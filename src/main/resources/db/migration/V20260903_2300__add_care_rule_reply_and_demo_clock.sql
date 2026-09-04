-- 버튼 응답도 재진입 시 동일한 내용으로 복원한다. Gemini 답변은 ai_reply에 별도 저장.
ALTER TABLE care_response ADD COLUMN rule_reply TEXT;
ALTER TABLE care_response ADD COLUMN request_id VARCHAR(80);
ALTER TABLE care_response ADD COLUMN request_payload TEXT;
CREATE UNIQUE INDEX uq_care_response_request ON care_response (care_signal_id, request_id)
    WHERE request_id IS NOT NULL;

-- 개발용 시연 날짜만 저장한다. 인증/JWT의 실제 시간에는 영향을 주지 않는다.
CREATE TABLE care_demo_state (
    member_id BIGINT PRIMARY KEY REFERENCES member(id) ON DELETE CASCADE,
    as_of TIMESTAMPTZ NOT NULL
);
