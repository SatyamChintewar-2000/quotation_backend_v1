-- ============================================================
-- V34: Consolidation of origin/develop address fields + local discount_amount
--
-- Absorbed from deleted V28__add_new_fields.sql and V29__add_new_fields_fix.sql
-- (those files conflicted with our V28__add_weight_cbm_columns.sql and are gone).
-- All ADD COLUMN statements use IF NOT EXISTS — fully idempotent.
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
    ADD COLUMN IF NOT EXISTS document_type    VARCHAR(30) DEFAULT 'INVOICE',
    ADD COLUMN IF NOT EXISTS customer_address TEXT,
    ADD COLUMN IF NOT EXISTS shipping_address TEXT,
    ADD COLUMN IF NOT EXISTS delivery_date    DATE,
    ADD COLUMN IF NOT EXISTS expiry_date      DATE;

-- Backfill document_type for existing rows
UPDATE invoice SET document_type = 'INVOICE' WHERE document_type IS NULL;

-- 4. quotation_items: flat discount amount for PDF display
--    (calculations always use discount_percentage; this is display-only)
ALTER TABLE quotation_items
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(15, 2) DEFAULT NULL;

-- 5. invoice: payment terms text
ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS payment_terms TEXT DEFAULT NULL;
