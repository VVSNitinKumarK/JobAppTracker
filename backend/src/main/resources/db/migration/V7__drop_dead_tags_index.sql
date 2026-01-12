-- Drop obsolete index from old schema design
-- Originally, company_tracking had a 'tags' column (likely text[] with GIN index)
-- Schema was refactored in V3 to use normalized junction tables (tag + company_tag) for optimization
-- The 'tags' column was removed, but V5 still references it
-- This migration cleans up the leftover index definition
DROP INDEX IF EXISTS jobapps.idx_company_tracking_tags_gin;