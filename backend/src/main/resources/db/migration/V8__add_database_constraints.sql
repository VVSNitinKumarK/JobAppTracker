-- V8__add_database_constraints.sql

-- 1) Unique constraint on tag.tag_key
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE n.nspname = 'jobapps'
          AND r.relname = 'tag'
          AND c.conname = 'uk_tag_key'
    ) THEN
        ALTER TABLE jobapps.tag
            ADD CONSTRAINT uk_tag_key UNIQUE (tag_key);
    END IF;
END $$;

-- 2) Primary key on company_tag (company_id, tag_id)
-- Only add if there is no primary key already
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE n.nspname = 'jobapps'
          AND r.relname = 'company_tag'
          AND c.contype = 'p'
    ) THEN
        ALTER TABLE jobapps.company_tag
            ADD CONSTRAINT pk_company_tag PRIMARY KEY (company_id, tag_id);
    END IF;
END $$;

-- 3) FK: company_tag.company_id -> company_tracking.company_id (ON DELETE CASCADE)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE n.nspname = 'jobapps'
          AND r.relname = 'company_tag'
          AND c.conname = 'fk_company_tag_company'
    ) THEN
        ALTER TABLE jobapps.company_tag
            ADD CONSTRAINT fk_company_tag_company
            FOREIGN KEY (company_id)
            REFERENCES jobapps.company_tracking(company_id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- 4) FK: company_tag.tag_id -> tag.tag_id (ON DELETE CASCADE)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE n.nspname = 'jobapps'
          AND r.relname = 'company_tag'
          AND c.conname = 'fk_company_tag_tag'
    ) THEN
        ALTER TABLE jobapps.company_tag
            ADD CONSTRAINT fk_company_tag_tag
            FOREIGN KEY (tag_id)
            REFERENCES jobapps.tag(tag_id)
            ON DELETE CASCADE;
    END IF;
END $$;
