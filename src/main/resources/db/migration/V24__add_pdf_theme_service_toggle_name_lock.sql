-- V24: PDF themes, service charges toggle, company name lock

-- Feature 1: PDF Theme (company-level)
ALTER TABLE company ADD COLUMN IF NOT EXISTS pdf_theme_name VARCHAR(50) DEFAULT 'navy';
ALTER TABLE company ADD COLUMN IF NOT EXISTS pdf_accent_color VARCHAR(7) DEFAULT '#1e3a8a';
ALTER TABLE company ADD COLUMN IF NOT EXISTS pdf_watermark_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE company ADD COLUMN IF NOT EXISTS pdf_watermark_opacity DECIMAL(3,2) DEFAULT 0.07;

-- Feature 2: Toggle service charges visibility on PDF (quotation-level)
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS hide_service_charges_on_pdf BOOLEAN DEFAULT FALSE;

-- Feature 3: Lock company name after first save
ALTER TABLE company ADD COLUMN IF NOT EXISTS company_name_locked BOOLEAN DEFAULT FALSE;
