-- ================================
-- JobAppTracker Baseline Schema
-- Squashed Flyway migration
-- ================================

CREATE SCHEMA IF NOT EXISTS jobapps;

-- ================================
-- Functions
-- ================================

CREATE OR REPLACE FUNCTION jobapps.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION jobapps.touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
RETURN NEW;
END;
$$;

-- ================================
-- Tables
-- ================================

CREATE TABLE jobapps.company (
                                 company_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 company_name TEXT NOT NULL,
                                 careers_url TEXT NOT NULL,
                                 last_visited_on DATE,
                                 revisit_after_days INT NOT NULL DEFAULT 7 CHECK (revisit_after_days > 0),
                                 next_visit_on DATE GENERATED ALWAYS AS (
                                     CASE
                                         WHEN last_visited_on IS NULL THEN NULL
                                         ELSE last_visited_on + revisit_after_days
                                         END
                                     ) STORED,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 CONSTRAINT uq_company_careers_url UNIQUE (careers_url)
);

CREATE TABLE jobapps.tag (
                             tag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             tag_name TEXT NOT NULL CHECK (btrim(tag_name) <> ''),
                             tag_key TEXT NOT NULL CHECK (btrim(tag_key) <> ''),
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             CONSTRAINT uk_tag_key UNIQUE (tag_key)
);

CREATE TABLE jobapps.company_tag (
                                     company_id UUID NOT NULL,
                                     tag_id UUID NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     CONSTRAINT pk_company_tag PRIMARY KEY (company_id, tag_id),
                                     CONSTRAINT fk_company_tag_company
                                         FOREIGN KEY (company_id)
                                             REFERENCES jobapps.company(company_id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_company_tag_tag
                                         FOREIGN KEY (tag_id)
                                             REFERENCES jobapps.tag(tag_id)
                                             ON DELETE CASCADE
);

CREATE TABLE jobapps.daily_checklist (
                                         check_date DATE NOT NULL,
                                         company_id UUID NOT NULL,
                                         completed BOOLEAN NOT NULL DEFAULT false,
                                         completed_at TIMESTAMPTZ,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         CONSTRAINT pk_daily_checklist PRIMARY KEY (check_date, company_id),
                                         CONSTRAINT fk_daily_checklist_company
                                             FOREIGN KEY (company_id)
                                                 REFERENCES jobapps.company(company_id)
                                                 ON DELETE CASCADE
);

-- ================================
-- Indexes
-- ================================

CREATE INDEX idx_company_next_visit_on
    ON jobapps.company (next_visit_on);

CREATE INDEX idx_company_company_name
    ON jobapps.company (company_name);

CREATE INDEX idx_company_tag_company_id
    ON jobapps.company_tag (company_id);

CREATE INDEX idx_company_tag_tag_id
    ON jobapps.company_tag (tag_id);

CREATE INDEX idx_daily_checklist_check_date
    ON jobapps.daily_checklist (check_date);

CREATE INDEX idx_daily_checklist_company_id
    ON jobapps.daily_checklist (company_id);

-- ================================
-- Triggers
-- ================================

CREATE TRIGGER trg_company_updated_at
    BEFORE UPDATE ON jobapps.company
    FOR EACH ROW
    EXECUTE FUNCTION jobapps.set_updated_at();

CREATE TRIGGER trg_tag_touch_updated_at
    BEFORE UPDATE ON jobapps.tag
    FOR EACH ROW
    EXECUTE FUNCTION jobapps.touch_updated_at();
