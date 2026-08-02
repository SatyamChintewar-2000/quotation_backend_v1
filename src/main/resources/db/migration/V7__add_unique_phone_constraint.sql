-- ============================================
-- V7: Add Unique Phone Constraint per Company
-- ============================================
-- This migration adds a unique constraint on phone number per company
-- to prevent duplicate customers with same phone in same company

-- Add unique constraint on phone per company
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_customer_phone_company'
    ) THEN
        ALTER TABLE customer
        ADD CONSTRAINT uk_customer_phone_company UNIQUE (phone, company_id);
    END IF;
END $$;
-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_customer_phone_company ON customer(phone, company_id) WHERE active = true;

-- ============================================
-- VERIFICATION
-- ============================================
-- Check constraint was added:
-- SELECT constraint_name FROM information_schema.table_constraints 
-- WHERE table_name = 'customer' AND constraint_type = 'UNIQUE';
