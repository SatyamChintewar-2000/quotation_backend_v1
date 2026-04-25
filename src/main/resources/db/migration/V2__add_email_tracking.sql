-- Add email tracking fields to quotation table
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS email_sent BOOLEAN DEFAULT FALSE;
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS email_sent_at TIMESTAMP;
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS email_status VARCHAR(50);
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS email_error_message TEXT;
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS last_reminder_sent_at TIMESTAMP;

-- Create email_log table for tracking all email communications
CREATE TABLE IF NOT EXISTS email_log (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT REFERENCES quotation(id),
    recipient_email VARCHAR(255) NOT NULL,
    email_type VARCHAR(50) NOT NULL, -- QUOTATION_SENT, APPROVED, REJECTED, EXPIRY_WARNING
    subject VARCHAR(500),
    body TEXT,
    status VARCHAR(50) NOT NULL, -- SUCCESS, FAILED, PENDING
    error_message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES "user"(id)
);

CREATE INDEX IF NOT EXISTS idx_email_log_quotation ON email_log(quotation_id);
CREATE INDEX IF NOT EXISTS idx_email_log_status ON email_log(status);
CREATE INDEX IF NOT EXISTS idx_email_log_type ON email_log(email_type);
