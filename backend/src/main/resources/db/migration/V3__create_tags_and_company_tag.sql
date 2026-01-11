-- Normalized tags
CREATE TABLE IF NOT EXISTS jobapps.tag (
  tag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tag_name TEXT NOT NULL,
  tag_key  TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Join table (company ↔ tag)
CREATE TABLE IF NOT EXISTS jobapps.company_tag (
  company_id UUID NOT NULL,
  tag_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
