-- Initial Schema for Quotation Application

-- Company Table
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Role Table
CREATE TABLE IF NOT EXISTS role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- Insert default roles
INSERT INTO role (role_name) VALUES ('SUPER_ADMIN'), ('CLIENT'), ('STAFF')
ON CONFLICT (role_name) DO NOTHING;

-- User Table
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id BIGINT REFERENCES role(id),
    company_id BIGINT REFERENCES company(id),
    active BOOLEAN DEFAULT TRUE,
    created_by BIGINT REFERENCES "user"(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES "user"(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES "user"(id)
);

-- Customer Table
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    company_id BIGINT NOT NULL REFERENCES company(id),
    created_by BIGINT NOT NULL REFERENCES "user"(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES "user"(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES "user"(id),
    UNIQUE(email, company_id)
);

-- Product Table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    tax_percentage DECIMAL(5, 2) DEFAULT 0,
    company_id BIGINT NOT NULL REFERENCES company(id),
    created_by BIGINT NOT NULL REFERENCES "user"(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES "user"(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES "user"(id)
);

-- Quotation Table
CREATE TABLE IF NOT EXISTS quotation (
    id BIGSERIAL PRIMARY KEY,
    quotation_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    company_id BIGINT NOT NULL REFERENCES company(id),
    created_by BIGINT NOT NULL REFERENCES "user"(id),
    status VARCHAR(20) DEFAULT 'DRAFT',
    total_amount DECIMAL(15, 2) DEFAULT 0,
    discount_percentage DECIMAL(5, 2) DEFAULT 0,
    expiry_date DATE NOT NULL,
    pdf_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES "user"(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT REFERENCES "user"(id)
);

-- Quotation Item Table
CREATE TABLE IF NOT EXISTS quotation_items (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES quotation(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    product_description TEXT,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    item_discount_percentage DECIMAL(5, 2) DEFAULT 0,
    tax_percentage DECIMAL(5, 2) DEFAULT 0,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    total DECIMAL(15, 2) NOT NULL
);

-- Quotation Status History Table
CREATE TABLE IF NOT EXISTS quotation_status_history (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES quotation(id),
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT NOT NULL REFERENCES "user"(id),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Quotation Archive Table
CREATE TABLE IF NOT EXISTS quotation_archive (
    id BIGSERIAL PRIMARY KEY,
    original_quotation_id BIGINT NOT NULL,
    quotation_number VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    customer_phone VARCHAR(50),
    customer_address TEXT,
    company_id BIGINT NOT NULL REFERENCES company(id),
    status VARCHAR(20),
    total_amount DECIMAL(15, 2),
    discount_percentage DECIMAL(5, 2),
    expiry_date DATE,
    pdf_path VARCHAR(500),
    items_snapshot JSONB,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    archived_by BIGINT NOT NULL REFERENCES "user"(id),
    archive_reason VARCHAR(255)
);

-- Refresh Token Table
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES "user"(id),
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Email Log Table
CREATE TABLE IF NOT EXISTS email_log (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT REFERENCES quotation(id),
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    email_type VARCHAR(50),
    status VARCHAR(20),
    error_message TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_email ON "user"(email);
CREATE INDEX IF NOT EXISTS idx_user_company ON "user"(company_id);
CREATE INDEX IF NOT EXISTS idx_customer_company ON customers(company_id);
CREATE INDEX IF NOT EXISTS idx_customer_created_by ON customers(created_by);
CREATE INDEX IF NOT EXISTS idx_product_company ON products(company_id);
CREATE INDEX IF NOT EXISTS idx_quotation_company ON quotation(company_id);
CREATE INDEX IF NOT EXISTS idx_quotation_customer ON quotation(customer_id);
CREATE INDEX IF NOT EXISTS idx_quotation_created_by ON quotation(created_by);
CREATE INDEX IF NOT EXISTS idx_quotation_status ON quotation(status);
CREATE INDEX IF NOT EXISTS idx_quotation_expiry ON quotation(expiry_date);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token(user_id);

-- Add missing columns to products table
ALTER TABLE products ADD COLUMN IF NOT EXISTS unit VARCHAR(50) DEFAULT 'piece';
ALTER TABLE products ADD COLUMN IF NOT EXISTS quantity INTEGER DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_percentage DECIMAL(5,2) DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS tax_type VARCHAR(20) DEFAULT 'GST';
ALTER TABLE products ADD COLUMN IF NOT EXISTS expiry_date DATE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_path VARCHAR(500);

-- Add missing columns to customers table
ALTER TABLE customers ADD COLUMN IF NOT EXISTS gst_number VARCHAR(50);

-- Add missing columns to users table
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS country_code VARCHAR(10) DEFAULT '+91';
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS department VARCHAR(100);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS avatar VARCHAR(500);

-- Add missing columns to quotation table
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS subtotal DECIMAL(15,2) DEFAULT 0;
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS total_discount DECIMAL(15,2) DEFAULT 0;
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS total_gst DECIMAL(15,2) DEFAULT 0;

-- Create additional indexes for better performance
CREATE INDEX IF NOT EXISTS idx_products_created_by ON products(created_by);
CREATE INDEX IF NOT EXISTS idx_products_company_extra ON products(company_id);
CREATE INDEX IF NOT EXISTS idx_customers_created_by_extra ON customers(created_by);
CREATE INDEX IF NOT EXISTS idx_quotation_customer_extra ON quotation(customer_id);
CREATE INDEX IF NOT EXISTS idx_quotation_created_by_extra ON quotation(created_by);
