-- V41: Add international (USD) purchase cost fields to products table
-- These fields support the "International Purchase" tab in product management.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS purchase_price_currency VARCHAR(3)   NOT NULL DEFAULT 'INR',
    ADD COLUMN IF NOT EXISTS purchase_price_usd       DECIMAL(12,4)         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS shipping_cost_usd        DECIMAL(10,4)         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS duty_gst_percent         DECIMAL(5,2)          DEFAULT 31.00,
    ADD COLUMN IF NOT EXISTS clearance_cost           DECIMAL(12,2)         DEFAULT NULL;
