-- V33: Add HSN/SAC code to products
-- HSN = Harmonized System of Nomenclature (for goods)
-- SAC = Services Accounting Code (for services)
-- Required for GST compliance on invoices and quotations

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS hsn_sac_code VARCHAR(20);
