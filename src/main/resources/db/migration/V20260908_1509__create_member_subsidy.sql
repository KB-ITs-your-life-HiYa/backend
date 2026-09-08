-- 회원이 현재 받고 있는 지원금. 우리 subsidy 카탈로그 안에서만 고를 수 있다
-- (자유 텍스트 입력은 지원하지 않는다 — 매칭 목록도 이 카탈로그 안에서만 뽑히니,
--  카탈로그 밖의 지원금은 애초에 중복될 일이 없어서 추적할 필요가 없다).
--
-- 용도: (1) 회원이 받고 있는 지원금을 스스로 확인 (2) 매칭 목록에서 이미 받고 있는
-- 지원금을 제외.

CREATE TABLE member_subsidy (
    member_id  BIGINT      NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    subsidy_id BIGINT      NOT NULL REFERENCES subsidy (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (member_id, subsidy_id)
);

COMMENT ON TABLE member_subsidy IS '회원이 현재 받고 있는 지원금. 매칭 목록에서 제외하는 데 쓴다';
