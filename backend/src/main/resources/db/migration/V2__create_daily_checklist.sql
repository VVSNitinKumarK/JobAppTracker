CREATE TABLE IF NOT EXISTS jobapps.daily_checklist (
    check_date      DATE NOT NULL,
    company_id      UUID NOT NULL
        REFERENCES  jobapps.company_tracking(company_id)
        ON DELETE CASCADE,
    completed       BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at    TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (check_date, company_id)
);

CREATE INDEX IF NOT EXISTS idx_daily_checklist_check_date
    ON jobapps.daily_checklist(check_date);

CREATE INDEX IF NOT EXISTS idx_daily_checklist_company_id
    ON jobapps.daily_checklist(company_id);