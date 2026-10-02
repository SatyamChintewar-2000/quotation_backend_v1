-- V45: Add domestic_shipping_inr to products table
-- For Indian/local products — per-unit domestic shipping cost in INR
-- Separate from import shipping (which is CBM-based for imported goods)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS domestic_shipping_inr DECIMAL(12, 2) DEFAULT 0;
