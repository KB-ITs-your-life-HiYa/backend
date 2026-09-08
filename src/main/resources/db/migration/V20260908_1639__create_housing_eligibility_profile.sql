CREATE TABLE housing_eligibility_profile (
    member_id BIGINT PRIMARY KEY REFERENCES member(id) ON DELETE CASCADE,
    is_homeless BOOLEAN NOT NULL,
    is_married BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
