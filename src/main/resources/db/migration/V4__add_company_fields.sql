-- Add missing columns to company table

-- Rename name to company_name
ALTER TABLE company RENAME COLUMN name TO company_name;

-- Add missing columns
ALTER TABLE company ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE company ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE company ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE company ADD COLUMN IF NOT EXISTS gst_number VARCHAR(50);
ALTER TABLE company ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE company ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Update existing companies to be active
UPDATE company SET active = TRUE WHERE active IS NULL;

-- Add index for active companies
CREATE INDEX IF NOT EXISTS idx_company_active ON company(active);
