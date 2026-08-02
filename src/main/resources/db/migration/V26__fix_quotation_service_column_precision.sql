-- V26: Fix quotation_service column precision
-- service_price was too small for large prices (overflow on values > 999.99)
-- service_tax was too small for percentages > 99.99 or large absolute values

ALTER TABLE quotation_service
    ALTER COLUMN service_price TYPE NUMERIC(15, 2),
    ALTER COLUMN service_tax TYPE NUMERIC(10, 2);
