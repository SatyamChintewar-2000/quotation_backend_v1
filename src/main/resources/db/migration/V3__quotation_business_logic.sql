-- Add quotation sequence table for auto-generating quotation numbers per company
CREATE TABLE IF NOT EXISTS quotation_sequence (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id),
    last_sequence INTEGER DEFAULT 0,
    year INTEGER NOT NULL,
    UNIQUE(company_id, year)
);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_quotation_sequence_company ON quotation_sequence(company_id);
CREATE INDEX IF NOT EXISTS idx_quotation_sequence_year ON quotation_sequence(year);

-- Add constraint to ensure quotation_number is unique per company
CREATE UNIQUE INDEX IF NOT EXISTS idx_quotation_number_company ON quotation(quotation_number, company_id) WHERE active = true;

-- Add check constraint for status values
ALTER TABLE quotation DROP CONSTRAINT IF EXISTS quotation_status_check;
ALTER TABLE quotation ADD CONSTRAINT quotation_status_check 
    CHECK (status IN ('DRAFT', 'GENERATED', 'SENT', 'APPROVED', 'REJECTED'));

-- Add currency column (default INR)
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'INR';

-- Add notes/remarks column
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS terms_and_conditions TEXT;

-- Add quotation_items snapshot fields for product details at time of quoting
ALTER TABLE quotation_items ADD COLUMN IF NOT EXISTS product_name_snapshot VARCHAR(255);
ALTER TABLE quotation_items ADD COLUMN IF NOT EXISTS product_description_snapshot TEXT;
ALTER TABLE quotation_items ADD COLUMN IF NOT EXISTS unit_snapshot VARCHAR(50);

-- Update quotation_items to store tax percentage
ALTER TABLE quotation_items ADD COLUMN IF NOT EXISTS tax_percentage DECIMAL(5,2) DEFAULT 0;

-- Add item-level discount
ALTER TABLE quotation_items ADD COLUMN IF NOT EXISTS discount_percentage DECIMAL(5,2) DEFAULT 0;

-- Add item total (calculated field, but stored for performance)
ALTER TABLE quotation_items ADD COLUMN IF NOT EXISTS item_total DECIMAL(15,2) DEFAULT 0;

-- Add indexes for quotation filtering
CREATE INDEX IF NOT EXISTS idx_quotation_status ON quotation(status) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_quotation_expiry ON quotation(expiry_date) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_quotation_created_at ON quotation(created_at) WHERE active = true;

-- Add function to check if quotation is expired
CREATE OR REPLACE FUNCTION is_quotation_expired(expiry_date DATE)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN expiry_date < CURRENT_DATE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Add function to get next quotation number
CREATE OR REPLACE FUNCTION get_next_quotation_number(p_company_id BIGINT)
RETURNS VARCHAR AS $$
DECLARE
    v_year INTEGER;
    v_sequence INTEGER;
    v_quotation_number VARCHAR;
BEGIN
    v_year := EXTRACT(YEAR FROM CURRENT_DATE);
    
    -- Insert or update sequence
    INSERT INTO quotation_sequence (company_id, year, last_sequence)
    VALUES (p_company_id, v_year, 1)
    ON CONFLICT (company_id, year)
    DO UPDATE SET last_sequence = quotation_sequence.last_sequence + 1
    RETURNING last_sequence INTO v_sequence;
    
    -- Format: QT-{companyId}-{sequence}
    v_quotation_number := 'QT-' || LPAD(p_company_id::TEXT, 3, '0') || '-' || LPAD(v_sequence::TEXT, 4, '0');
    
    RETURN v_quotation_number;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE quotation_sequence IS 'Stores the last sequence number for quotation numbering per company per year';
COMMENT ON FUNCTION get_next_quotation_number IS 'Generates unique quotation number in format QT-{companyId}-{sequence}';
COMMENT ON FUNCTION is_quotation_expired IS 'Checks if a quotation has expired based on expiry_date';
