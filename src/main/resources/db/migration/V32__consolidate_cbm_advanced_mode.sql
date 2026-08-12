-- V32: Consolidate CBM/Weight/USD toggles into single "Advanced Options (CBM)" toggle
--      Add shipping cost, clearance per CBM, and installation cost fields
--
-- Replaces: show_weight_column, show_cbm_column, show_usd_column (kept for backward compat)
-- Adds: cbm_advanced_mode (single master toggle)
--       shipping_cost_usd    — today's shipping cost in USD (entered by admin)
--       clearance_per_cbm    — customs clearance cost per CBM in INR
--       installation_cost    — flat installation cost in INR

ALTER TABLE company
    ADD COLUMN IF NOT EXISTS cbm_advanced_mode   BOOLEAN        DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS shipping_cost_usd   DECIMAL(10,2)  DEFAULT 0,
    ADD COLUMN IF NOT EXISTS clearance_per_cbm   DECIMAL(10,2)  DEFAULT 1667,
    ADD COLUMN IF NOT EXISTS installation_cost   DECIMAL(12,2)  DEFAULT 0;

-- Migrate: any company that had any of the old toggles on gets advanced mode on
UPDATE company
SET cbm_advanced_mode = TRUE
WHERE show_cbm_column = TRUE OR show_usd_column = TRUE OR show_weight_column = TRUE;
