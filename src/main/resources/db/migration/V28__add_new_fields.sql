-- ============================================================
-- V28: Add shipping address to customer table,
--      address/shipping snapshots + documentType to invoice,
--      address/shipping snapshots to quotation table
-- ============================================================

-- 1. customer: shipping address
ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS shipping_address TEXT;

-- 2. quotation: customer address + shipping address snapshots
ALTER TABLE quotation
    ADD COLUMN IF NOT EXISTS customer_address TEXT,
    ADD COLUMN IF NOT EXISTS shipping_address TEXT;

-- 3. invoice: document type, address snapshots, delivery/expiry dates
ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(30) DEFAULT 'INVOICE',
    ADD COLUMN IF NOT EXISTS customer_address TEXT,
    ADD COLUMN IF NOT EXISTS shipping_address TEXT,
    ADD COLUMN IF NOT EXISTS delivery_date DATE,
    ADD COLUMN IF NOT EXISTS expiry_date DATE;

-- Backfill document_type for existing rows
UPDATE invoice SET document_type = 'INVOICE' WHERE document_type IS NULL;
