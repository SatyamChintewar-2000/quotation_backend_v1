-- Increase setting_value column size to support base64-encoded images (logos)
ALTER TABLE app_settings
ALTER COLUMN setting_value TYPE TEXT;
