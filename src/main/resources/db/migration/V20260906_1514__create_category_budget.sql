-- =====================================================================
-- 카테고리별 월 예산
--   monthly_budget(총 예산)에 종속. 총 예산 없이는 존재할 수 없음.
--   행이 없는 카테고리 = 예산 미설정 (리포트에서 지난달 대비로 폴백)
-- =====================================================================

CREATE TABLE monthly_category_budget (
                                         id                BIGSERIAL    PRIMARY KEY,
                                         monthly_budget_id BIGINT       NOT NULL,
                                         category          VARCHAR(30)  NOT NULL,
                                         amount            BIGINT       NOT NULL,
                                         created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                         updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

                                         CONSTRAINT fk_monthly_category_budget_budget
                                             FOREIGN KEY (monthly_budget_id)
                                                 REFERENCES monthly_budget (id) ON DELETE CASCADE,

                                         CONSTRAINT uq_monthly_category_budget
                                             UNIQUE (monthly_budget_id, category),

                                         CONSTRAINT ck_monthly_category_budget_amount_positive
                                             CHECK (amount > 0)
);

CREATE INDEX idx_monthly_category_budget_budget_id
    ON monthly_category_budget (monthly_budget_id);

COMMENT ON TABLE  monthly_category_budget IS '카테고리별 월 예산. 행이 없으면 해당 카테고리는 예산 미설정';
COMMENT ON COLUMN monthly_category_budget.category IS 'HOUSING_UTILITY, FOOD, LEISURE_SHOPPING, LIVING_MEDICAL, TRANSPORT';