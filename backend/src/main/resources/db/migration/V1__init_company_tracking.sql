-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jobapps;

-- Main company tracking table
CREATE TABLE IF NOT EXISTS jobapps.company_tracking (
    company_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name TEXT NOT NULL,
    careers_url TEXT UNIQUE,
    last_visited_on DATE,
    revisit_after_days INT NOT NULL,
    next_visit_on DATE GENERATED ALWAYS AS (
        CASE
            WHEN last_visited_on IS NOT NULL THEN last_visited_on + revisit_after_days
            ELSE NULL
        END
    ) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index on company name for search queries
CREATE INDEX IF NOT EXISTS idx_company_tracking_company_name
    ON jobapps.company_tracking(company_name);

-- Index on next visit date for filtering due/overdue companies
CREATE INDEX IF NOT EXISTS idx_company_tracking_next_visit_on
    ON jobapps.company_tracking(next_visit_on);
