-- 1. 원본 보관용 (API 응답 그대로)
CREATE TABLE subsidy_raw (
     id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
     source       TEXT NOT NULL,
     external_id  TEXT NOT NULL,
     raw_payload  JSONB NOT NULL,
     fetched_at   TIMESTAMP NOT NULL DEFAULT now(),

     UNIQUE (source, external_id)
);

-- 2. 파싱된 지원금 통합 테이블
CREATE TABLE subsidy (
     id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
     raw_id                      BIGINT REFERENCES subsidy_raw (id),

     source                      TEXT NOT NULL,
     external_id                 TEXT NOT NULL,

     name                        TEXT NOT NULL,
     summary                     TEXT,
     org_name                    TEXT,

     category_raw                TEXT,
     category                    TEXT,

     target_raw                  TEXT,
     benefit_raw                 TEXT,

     apply_method                TEXT,
     apply_deadline_raw          TEXT,
     apply_deadline_date         DATE,

     biz_start_date              DATE,
     biz_end_date                DATE,

     detail_url                  TEXT,

     min_age                     INTEGER,
     max_age                     INTEGER,

     income_pct_max              INTEGER,
     income_amt_min              BIGINT,
     income_amt_max              BIGINT,

     protection_status_required  TEXT,
     min_years_after_end         NUMERIC,
     max_years_after_end         NUMERIC,

     target_household            TEXT[],

     exclusion_group             TEXT,
     duplicate_of_subsidy_id     BIGINT REFERENCES subsidy (id),

     needs_manual_review         BOOLEAN NOT NULL DEFAULT true,
     created_at                  TIMESTAMP NOT NULL DEFAULT now(),

     UNIQUE (source, external_id)
);

-- 3. 지원금별 세부 혜택 항목 (한 지원금에 혜택 여러 개 가능)
CREATE TABLE subsidy_benefit (
     id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
     subsidy_id    BIGINT NOT NULL REFERENCES subsidy (id),

     benefit_name  TEXT NOT NULL,
     amount_krw    BIGINT,
     cycle         TEXT
);

-- 4. 지원금 적용 지역 (row 없으면 전국)
CREATE TABLE subsidy_region (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subsidy_id     BIGINT NOT NULL REFERENCES subsidy (id),

    sido_code      TEXT NOT NULL,
    sigungu_code   TEXT
);