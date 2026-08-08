-- ============================================================
-- V29: Idempotent re-check — ensures all V28 columns exist.
--      Safe to run even if V28 partially applied.
-- ============================================================

ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS shipping_address TEXT;

ALTER TABLE quotation
    ADD COLUMN IF NOT EXISTS customer_address TEXT,
    ADD COLUMN IF NOT EXISTS shipping_address TEXT;

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(30) DEFAULT 'INVOICE',
    ADD COLUMN IF NOT EXISTS customer_address TEXT,
    ADD COLUMN IF NOT EXISTS shipping_address TEXT,
    ADD COLUMN IF NOT EXISTS delivery_date DATE,
    ADD COLUMN IF NOT EXISTS expiry_date DATE;

UPDATE invoice SET document_type = 'INVOICE' WHERE document_type IS NULL;
