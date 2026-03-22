-- V8: Add Invoice Feature
-- Creates tables for invoice management: invoice, invoice_items, invoice_payments, invoice_sequence

-- Invoice Sequence Table (for auto-generating invoice numbers)
CREATE TABLE IF NOT EXISTS invoice_sequence (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL REFERENCES company(id),
  last_sequence INTEGER DEFAULT 0,
  year INTEGER NOT NULL,
  UNIQUE(company_id, year)
);

-- Invoice Table
CREATE TABLE IF NOT EXISTS invoice (
  id BIGSERIAL PRIMARY KEY,
  invoice_number VARCHAR(50) NOT NULL UNIQUE,
  quotation_id BIGINT NOT NULL REFERENCES quotation(id),
  customer_id BIGINT NOT NULL REFERENCES customer(id),
  company_id BIGINT NOT NULL REFERENCES company(id),
  
  invoice_date DATE NOT NULL,
  due_date DATE NOT NULL,
  
  subtotal DECIMAL(15,2) DEFAULT 0,
  discount_percentage DECIMAL(5,2) DEFAULT 0,
  total_discount DECIMAL(15,2) DEFAULT 0,
  total_tax DECIMAL(15,2) DEFAULT 0,
  total_amount DECIMAL(15,2) DEFAULT 0,
  
  status VARCHAR(20) DEFAULT 'DRAFT',
  payment_status VARCHAR(20) DEFAULT 'PENDING',
  
  notes TEXT,
  terms_and_conditions TEXT,
  
  email_sent BOOLEAN DEFAULT FALSE,
  email_sent_at TIMESTAMP,
  
  created_by BIGINT NOT NULL REFERENCES "user"(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT REFERENCES "user"(id),
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  active BOOLEAN DEFAULT TRUE,
  deleted_at TIMESTAMP,
  deleted_by BIGINT REFERENCES "user"(id)
);

-- Invoice Items Table
CREATE TABLE IF NOT EXISTS invoice_items (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES products(id),
  
  product_name VARCHAR(255) NOT NULL,
  product_description TEXT,
  quantity INTEGER NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  discount_percentage DECIMAL(5,2) DEFAULT 0,
  tax_percentage DECIMAL(5,2) DEFAULT 0,
  tax_amount DECIMAL(15,2) DEFAULT 0,
  item_total DECIMAL(15,2) DEFAULT 0,
  total DECIMAL(15,2) NOT NULL
);

-- Invoice Payments Table
CREATE TABLE IF NOT EXISTS invoice_payments (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoice(id),
  
  payment_date DATE NOT NULL,
  payment_amount DECIMAL(15,2) NOT NULL,
  payment_method VARCHAR(50),
  payment_reference VARCHAR(100),
  notes TEXT,
  
  created_by BIGINT NOT NULL REFERENCES "user"(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Indexes for Performance
CREATE INDEX IF NOT EXISTS idx_invoice_company_id ON invoice(company_id);
CREATE INDEX IF NOT EXISTS idx_invoice_customer_id ON invoice(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoice_quotation_id ON invoice(quotation_id);
CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoice(status);
CREATE INDEX IF NOT EXISTS idx_invoice_payment_status ON invoice(payment_status);
CREATE INDEX IF NOT EXISTS idx_invoice_created_by ON invoice(created_by);
CREATE INDEX IF NOT EXISTS idx_invoice_invoice_date ON invoice(invoice_date);
CREATE INDEX IF NOT EXISTS idx_invoice_due_date ON invoice(due_date);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_product_id ON invoice_items(product_id);

CREATE INDEX IF NOT EXISTS idx_invoice_payments_invoice_id ON invoice_payments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_payments_payment_date ON invoice_payments(payment_date);

CREATE INDEX IF NOT EXISTS idx_invoice_sequence_company_id ON invoice_sequence(company_id);
