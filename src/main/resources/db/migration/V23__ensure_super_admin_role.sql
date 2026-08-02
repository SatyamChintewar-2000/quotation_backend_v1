-- Ensure SUPER_ADMIN role exists
-- This migration ensures the SUPER_ADMIN role is present in the database
-- regardless of how the initial migration ran

-- Insert SUPER_ADMIN role if it doesn't exist
INSERT INTO role (role_name)
VALUES ('SUPER_ADMIN')
ON CONFLICT (role_name) DO NOTHING;

-- Ensure CLIENT and STAFF roles exist too
INSERT INTO role (role_name)
VALUES ('CLIENT')
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO role (role_name)
VALUES ('STAFF')
ON CONFLICT (role_name) DO NOTHING;

-- Log current roles for verification
-- SELECT * FROM role;
