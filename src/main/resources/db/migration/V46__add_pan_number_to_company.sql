-- V46: Add PAN number field to company profile
-- PAN format: 10 characters (e.g. ABCDE1234F)
ALTER TABLE company
    ADD COLUMN IF NOT EXISTS pan_number VARCHAR(10);
