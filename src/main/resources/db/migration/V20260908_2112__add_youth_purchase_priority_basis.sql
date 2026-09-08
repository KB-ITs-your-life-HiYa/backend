ALTER TABLE housing_eligibility_profile
    ADD COLUMN youth_purchase_priority_basis VARCHAR(30);

ALTER TABLE housing_eligibility_profile
    ADD CONSTRAINT ck_housing_eligibility_youth_purchase_priority
        CHECK (youth_purchase_priority_basis IS NULL OR youth_purchase_priority_basis IN (
            'BENEFIT_RECIPIENT',
            'SUPPORTED_SINGLE_PARENT',
            'NEAR_POVERTY',
            'NONE'
        ));

COMMENT ON COLUMN housing_eligibility_profile.youth_purchase_priority_basis
    IS 'LH 청년 매입임대 1순위 근거. NONE은 세 항목 모두 해당 없음';
