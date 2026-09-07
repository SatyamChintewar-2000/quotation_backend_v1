-- V43: Add stack_weight_snapshot to quotation_items
-- Captures the gross/stack weight at quotation creation time
ALTER TABLE quotation_master.quotation_items
    ADD COLUMN IF NOT EXISTS stack_weight_snapshot NUMERIC(10, 3);
