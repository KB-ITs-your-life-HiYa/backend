-- housing_type 에 "일시 거주 / 거처 불안정" 값 추가.
--
-- 기존 값(자가·전세·월세·무상거주·자립생활관 등·공공임대)은 전부 정식 점유 형태를
-- 전제로 해서, 지인 집에 잠깐 얹혀 지내는 등 거처가 불안정한 경우를 표현할 수 없었다.
-- 보호종료 청년에게 드물지 않은 상황이라 별도 값으로 추가한다.
--
--   OWNED 자가 · JEONSE 전세 · MONTHLY_RENT 월세 · FREE 무상거주
--   SELF_RELIANCE_HOUSE 자립생활관 등 · PUBLIC_RENTAL 공공임대
--   UNSTABLE 일시 거주 / 거처 불안정

ALTER TABLE member_survey DROP CONSTRAINT ck_member_survey_housing_type;

ALTER TABLE member_survey ADD CONSTRAINT ck_member_survey_housing_type
    CHECK (housing_type IS NULL
           OR housing_type IN ('OWNED', 'JEONSE', 'MONTHLY_RENT', 'FREE',
                               'SELF_RELIANCE_HOUSE', 'PUBLIC_RENTAL', 'UNSTABLE'));
