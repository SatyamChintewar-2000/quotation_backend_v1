ALTER TABLE quotation
    ADD COLUMN IF NOT EXISTS quotation_date DATE,
    ADD COLUMN IF NOT EXISTS quotation_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS delivery_date DATE,
    ADD COLUMN IF NOT EXISTS executive_name VARCHAR(100);

CREATE TABLE IF NOT EXISTS quotation_service (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES quotation(id) ON DELETE CASCADE,
    service_name VARCHAR(200) NOT NULL,
    service_price NUMERIC(15,2) DEFAULT 0,
    service_tax NUMERIC(5,2) DEFAULT 0
);
