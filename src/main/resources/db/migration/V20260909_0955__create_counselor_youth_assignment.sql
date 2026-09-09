ALTER TABLE member
    ADD COLUMN name VARCHAR(50),
    ADD COLUMN phone VARCHAR(20);

-- 로그인은 member가 담당하고, 상담사 업무 정보는 기존 counselor가 담당한다.
ALTER TABLE counselor
    ADD COLUMN member_id BIGINT REFERENCES member (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uq_counselor_member
    ON counselor (member_id)
    WHERE member_id IS NOT NULL;

CREATE TABLE counselor_youth_assignment (
    id              BIGSERIAL   PRIMARY KEY,
    counselor_id    BIGINT      NOT NULL REFERENCES counselor (id) ON DELETE CASCADE,
    youth_member_id BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    unassigned_at   TIMESTAMPTZ,

    CONSTRAINT ck_counselor_youth_assignment_period
        CHECK (unassigned_at IS NULL OR unassigned_at >= assigned_at)
);

-- 한 학생에게 동시에 두 명의 담당 상담사가 배정되지 않게 한다.
CREATE UNIQUE INDEX uq_counselor_youth_active_assignment
    ON counselor_youth_assignment (youth_member_id)
    WHERE unassigned_at IS NULL;

CREATE INDEX ix_counselor_youth_active_by_counselor
    ON counselor_youth_assignment (counselor_id, assigned_at DESC)
    WHERE unassigned_at IS NULL;

COMMENT ON COLUMN counselor.member_id IS '로그인에 사용하는 member 상담사 계정';
COMMENT ON TABLE counselor_youth_assignment IS '상담사와 담당 자립청년의 배정 이력';
COMMENT ON COLUMN counselor_youth_assignment.unassigned_at IS 'NULL이면 현재 활성 배정';
