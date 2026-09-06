-- ============================================================
-- V39: Backfill hsn_code on invoice_items from the products table.
--      Invoice items created before the HSN fix have null hsnCode.
--      This copies hsnCode (or falls back to hsnSacCode) from
--      the linked product into the invoice item row.
-- ============================================================
UPDATE invoice_items ii
SET hsn_code = COALESCE(p.hsn_code, p.hsn_sac_code)
FROM products p
WHERE ii.product_id = p.id
  AND ii.hsn_code IS NULL
  AND COALESCE(p.hsn_code, p.hsn_sac_code) IS NOT NULL;
