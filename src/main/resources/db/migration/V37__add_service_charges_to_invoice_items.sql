-- ============================================================
-- V37: Support service/installation charges as line items
--      on invoices. Makes product_id nullable so service rows
--      don't need a product record, and adds an item_type
--      discriminator ('PRODUCT' | 'SERVICE').
-- PostgreSQL syntax.
-- ============================================================

-- Make product_id nullable (service rows have no product)
ALTER TABLE invoice_items
    ALTER COLUMN product_id DROP NOT NULL;

-- Add item type column — defaults to 'PRODUCT' so existing rows are unchanged
ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS item_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCT';

-- Backfill existing rows
UPDATE invoice_items SET item_type = 'PRODUCT' WHERE item_type IS NULL OR item_type = '';
