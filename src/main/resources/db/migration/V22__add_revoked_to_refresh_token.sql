-- Add revoked column to refresh_token table
-- Purpose: Track revoked refresh tokens for security

-- Add revoked column (nullable initially, then set default)
ALTER TABLE refresh_token 
ADD COLUMN IF NOT EXISTS revoked BOOLEAN;

-- Set default value for existing rows
UPDATE refresh_token 
SET revoked = false 
WHERE revoked IS NULL;

-- Make column NOT NULL with default
ALTER TABLE refresh_token 
ALTER COLUMN revoked SET DEFAULT false;

ALTER TABLE refresh_token 
ALTER COLUMN revoked SET NOT NULL;

-- Add comment
COMMENT ON COLUMN refresh_token.revoked IS 'Flag to track if refresh token has been revoked';
