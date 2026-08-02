-- Add bank details columns to company table for payment information in quotations/invoices

ALTER TABLE company 
ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS account_number VARCHAR(100),
ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS branch_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100);

-- Add comments for documentation
COMMENT ON COLUMN company.bank_name IS 'Bank name for payment details';
COMMENT ON COLUMN company.account_number IS 'Bank account number';
COMMENT ON COLUMN company.ifsc_code IS 'IFSC code for bank transfers';
COMMENT ON COLUMN company.branch_name IS 'Bank branch name';
COMMENT ON COLUMN company.upi_id IS 'UPI ID for QR code payment generation';
