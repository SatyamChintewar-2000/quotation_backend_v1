-- V36: Make customer email column nullable.
-- Email is optional — customers can be created without an email address.

ALTER TABLE customer
    ALTER COLUMN email DROP NOT NULL;
