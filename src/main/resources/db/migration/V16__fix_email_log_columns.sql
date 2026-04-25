-- Add missing columns to email_log table
ALTER TABLE email_log ADD COLUMN IF NOT EXISTS body TEXT;
ALTER TABLE email_log ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES "user"(id);
ALTER TABLE email_log ADD COLUMN IF NOT EXISTS email_type VARCHAR(50);

-- Fix status column to support enum values used by the application
ALTER TABLE email_log ALTER COLUMN status TYPE VARCHAR(50);
