-- V28: Add Net Weight and CBM (Cubic Metre) fields for export/logistics quotations
--
-- These are OPTIONAL fields — not required for standard quotations.
-- They allow companies (e.g. fitness equipment exporters) to include
-- shipping weight and volume data in their quotations and PDF output.

-- Products: store weight and volume per unit
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS net_weight  DECIMAL(10,3),   -- kg per unit
    ADD COLUMN IF NOT EXISTS cbm         DECIMAL(10,4);   -- cubic metres per unit

-- Company: toggle columns on/off per company + freight rate config
ALTER TABLE company
    ADD COLUMN IF NOT EXISTS show_weight_column  BOOLEAN        DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS show_cbm_column     BOOLEAN        DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS show_usd_column     BOOLEAN        DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS rate_per_cbm        DECIMAL(10,2)  DEFAULT 0,
    ADD COLUMN IF NOT EXISTS usd_exchange_rate   DECIMAL(10,2)  DEFAULT 83;

-- Quotation items: capture snapshot at time of quoting (immutable audit trail)
ALTER TABLE quotation_items
    ADD COLUMN IF NOT EXISTS net_weight_snapshot DECIMAL(10,3),
    ADD COLUMN IF NOT EXISTS cbm_snapshot        DECIMAL(10,4);
