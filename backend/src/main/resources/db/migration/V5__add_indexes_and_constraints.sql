-- company_tracking
CREATE INDEX IF NOT EXISTS idx_company_tracking_next_visit_on
  ON jobapps.company_tracking(next_visit_on);

CREATE INDEX IF NOT EXISTS idx_company_tracking_tags_gin
  ON jobapps.company_tracking USING GIN (tags);

-- daily_checklist
CREATE INDEX IF NOT EXISTS idx_daily_checklist_check_date
  ON jobapps.daily_checklist(check_date);

CREATE INDEX IF NOT EXISTS idx_daily_checklist_company_id
  ON jobapps.daily_checklist(company_id);

-- company_tag
CREATE INDEX IF NOT EXISTS idx_company_tag_company_id
  ON jobapps.company_tag(company_id);

CREATE INDEX IF NOT EXISTS idx_company_tag_tag_id
  ON jobapps.company_tag(tag_id);

-- tag
CREATE INDEX IF NOT EXISTS idx_tag_key
  ON jobapps.tag(tag_key);
