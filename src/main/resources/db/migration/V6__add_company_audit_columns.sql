-- Add audit columns to company table
ALTER TABLE company ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE company ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE company ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
ALTER TABLE company ADD COLUMN IF NOT EXISTS logo TEXT;

-- Add foreign key constraints only if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_company_created_by'
    ) THEN
        ALTER TABLE company ADD CONSTRAINT fk_company_created_by 
            FOREIGN KEY (created_by) REFERENCES "user"(id);
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_company_updated_by'
    ) THEN
        ALTER TABLE company ADD CONSTRAINT fk_company_updated_by 
            FOREIGN KEY (updated_by) REFERENCES "user"(id);
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_company_deleted_by'
    ) THEN
        ALTER TABLE company ADD CONSTRAINT fk_company_deleted_by 
            FOREIGN KEY (deleted_by) REFERENCES "user"(id);
    END IF;
END $$;
