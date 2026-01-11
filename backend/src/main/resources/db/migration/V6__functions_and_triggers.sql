-- Required extension (safe if already enabled)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Updated-at trigger function
CREATE OR REPLACE FUNCTION jobapps.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
