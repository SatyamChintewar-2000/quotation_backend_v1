-- V44: Add duty_gst_percent to company table
-- Allows each company to configure their import GST+duty % (default 31%)
-- Used in Export Cost Analysis panel and CBM Excel export
ALTER TABLE company
    ADD COLUMN IF NOT EXISTS duty_gst_percent DECIMAL(5, 2) DEFAULT 31.00;
