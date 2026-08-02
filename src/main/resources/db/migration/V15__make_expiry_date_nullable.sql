-- Make expiry_date nullable since it is optional in the application
ALTER TABLE quotation ALTER COLUMN expiry_date DROP NOT NULL;
