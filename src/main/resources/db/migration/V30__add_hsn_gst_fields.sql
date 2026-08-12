-- ============================================================
-- V30: Add HSN/SAC code to products and invoice_items,
--      Add gst_type to invoice table
-- ============================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(20);

ALTER TABLE invoice_items
    ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(20);

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS gst_type VARCHAR(20) DEFAULT 'SGST_CGST';

UPDATE invoice SET gst_type = 'SGST_CGST' WHERE gst_type IS NULL;
