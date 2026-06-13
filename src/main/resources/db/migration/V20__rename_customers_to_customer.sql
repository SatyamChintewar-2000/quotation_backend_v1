-- Rename customers table to customer to match Entity class
-- This fixes the schema validation error where Entity expects 'customer' but table is 'customers'

-- Step 1: Rename the table
ALTER TABLE IF EXISTS customers RENAME TO customer;

-- Step 2: Rename any indexes that reference the old table name
-- The unique constraint index will be automatically renamed by PostgreSQL

-- Step 3: Add comment for documentation
COMMENT ON TABLE customer IS 'Customer information - renamed from customers to match Entity class @Table(name = "customer")';
