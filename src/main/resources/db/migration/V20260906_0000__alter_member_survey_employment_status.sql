-- employment_status 값 재설계.
--
-- SEEKING(구직중) 이 UNEMPLOYED(미취업) 와 의미가 겹쳐 설문에서 고르기 애매했다.
-- SH·LH 청년 전형 등에서 실제로 쓰는 "졸업(중퇴) 후 2년 이내 구직중" 이라는
-- 구체적인 취업준비생 기준으로 대체한다. 나머지 값은 그대로 둔다.
--
--   EMPLOYED 재직중 · SELF_EMPLOYED 자영업 · STUDENT 재학중
--   JOB_SEEKER 취업준비생(졸업·중퇴 후 2년 이내) · UNEMPLOYED 무직(그 외)

-- 새 제약을 걸기 전에 기존 SEEKING 값을 UNEMPLOYED 로 옮긴다.
-- 이 DB 에 SEEKING 으로 저장된 행이 있어도 ADD CONSTRAINT 가 실패하지 않도록.
UPDATE member_survey SET employment_status = 'UNEMPLOYED' WHERE employment_status = 'SEEKING';

ALTER TABLE member_survey DROP CONSTRAINT ck_member_survey_employment_status;

ALTER TABLE member_survey ADD CONSTRAINT ck_member_survey_employment_status
    CHECK (employment_status IS NULL
           OR employment_status IN ('EMPLOYED', 'SELF_EMPLOYED', 'STUDENT', 'JOB_SEEKER', 'UNEMPLOYED'));
