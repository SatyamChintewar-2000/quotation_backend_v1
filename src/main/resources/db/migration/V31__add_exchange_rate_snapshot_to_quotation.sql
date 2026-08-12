-- V31: Snapshot exchange rate and CBM rate at quotation creation time
--
-- WHY: Exchange rates and CBM freight rates change daily.
-- A quotation is a legal price offer — once created, the rates must be frozen.
-- Viewing or reprinting an old quotation must show the ORIGINAL rates, not today's.
--
-- These values are captured from company settings at the moment the quotation is saved.

ALTER TABLE quotation
    ADD COLUMN IF NOT EXISTS usd_exchange_rate_snapshot  DECIMAL(10,2),  -- INR per $1 at time of quoting
    ADD COLUMN IF NOT EXISTS rate_per_cbm_snapshot       DECIMAL(10,2);  -- USD per m3 at time of quoting
