CREATE TABLE IF NOT EXISTS app_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(500),
    updated_at TIMESTAMP DEFAULT NOW(),
    updated_by BIGINT
);

-- Default notification settings
INSERT INTO app_settings (setting_key, setting_value, description) VALUES
('email_notifications_enabled', 'true', 'Enable or disable all email notifications'),
('whatsapp_notifications_enabled', 'false', 'Enable or disable WhatsApp notifications'),
('whatsapp_api_url', '', 'WhatsApp API endpoint URL'),
('whatsapp_api_token', '', 'WhatsApp API authentication token'),
('whatsapp_phone_number_id', '', 'WhatsApp Business phone number ID');
