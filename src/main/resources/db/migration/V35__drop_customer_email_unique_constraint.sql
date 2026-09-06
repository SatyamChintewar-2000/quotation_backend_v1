-- V35: Drop the email uniqueness constraint on customer table.
-- Email is no longer unique — multiple staff in the same company can add the same customer.
-- Phone uniqueness is now enforced at the staff (createdBy) level in application code only.

-- Works for both quotation_master schema (local) and public schema (production)
ALTER TABLE customer
    DROP CONSTRAINT IF EXISTS customers_email_company_id_key;

ALTER TABLE customer
    DROP CONSTRAINT IF EXISTS uk_customer_phone_company;
