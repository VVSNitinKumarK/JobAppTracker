ALTER TABLE jobapps.daily_checklist
ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
