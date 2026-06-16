-- V27: Add subscription expiry date to company
-- Super Admin sets this per company; when expired, CLIENT/STAFF users are blocked at login
ALTER TABLE company ADD COLUMN IF NOT EXISTS subscription_expires_at DATE DEFAULT NULL;
