-- Add unique constraint on tag.tag_key to prevent duplicate normalized tags
ALTER TABLE jobapps.tag
    ADD CONSTRAINT uk_tag_key UNIQUE (tag_key);

-- Add primary key to company_tag junction table
-- This ensures no duplicate company-tag pairs
ALTER TABLE jobapps.company_tag
    ADD CONSTRAINT pk_company_tag PRIMARY KEY (company_id, tag_id);

-- Add foreign key constraint from company_tag to company_tracking
-- ON DELETE CASCADE ensures orphaned tags are cleaned up when a company is deleted
ALTER TABLE jobapps.company_tag
    ADD CONSTRAINT fk_company_tag_company
    FOREIGN KEY (company_id)
    REFERENCES jobapps.company_tracking(company_id)
    ON DELETE CASCADE;

-- Add foreign key constraint from company_tag to tag
-- ON DELETE CASCADE ensures orphaned mappings are cleaned up when a tag is deleted
ALTER TABLE jobapps.company_tag
    ADD CONSTRAINT fk_company_tag_tag
    FOREIGN KEY (tag_id)
    REFERENCES jobapps.tag(tag_id)
    ON DELETE CASCADE;
