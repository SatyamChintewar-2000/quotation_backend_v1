-- V42: Add stack_weight column to products table
-- Stack weight = gross weight including packaging/stacking per unit (kg)
ALTER TABLE quotation_master.products
    ADD COLUMN IF NOT EXISTS stack_weight NUMERIC(10, 3);
