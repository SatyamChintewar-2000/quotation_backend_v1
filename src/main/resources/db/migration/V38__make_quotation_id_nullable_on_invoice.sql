-- ============================================================
-- V38: Make quotation_id nullable on invoice table.
--      Direct invoices have no parent quotation, so this
--      column must allow NULL.
-- ============================================================
ALTER TABLE invoice ALTER COLUMN quotation_id DROP NOT NULL;
