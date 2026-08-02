-- V25: Add License ID to company for client identification
-- License ID is auto-generated on creation, never changes
ALTER TABLE company ADD COLUMN IF NOT EXISTS license_id VARCHAR(30) UNIQUE;

-- Generate license IDs for existing companies that don't have one
-- Format: QF-{YEAR}-{3 LETTERS}-{4 DIGIT SEQ}
DO $$
DECLARE
    rec RECORD;
    seq INT := 1;
    prefix CHAR(3);
    new_id VARCHAR(30);
BEGIN
    FOR rec IN SELECT id, company_name, created_at FROM company ORDER BY id LOOP
        IF rec.company_name IS NOT NULL AND length(trim(rec.company_name)) >= 3 THEN
            prefix := upper(substring(regexp_replace(rec.company_name, '[^a-zA-Z]', '', 'g'), 1, 3));
        ELSE
            prefix := 'QFL';
        END IF;
        IF length(prefix) < 3 THEN
            prefix := rpad(prefix, 3, 'X');
        END IF;
        new_id := 'QF-' || to_char(COALESCE(rec.created_at, NOW()), 'YYYY') || '-' || prefix || '-' || lpad(seq::text, 4, '0');
        -- Ensure uniqueness
        WHILE EXISTS (SELECT 1 FROM company WHERE license_id = new_id) LOOP
            seq := seq + 1;
            new_id := 'QF-' || to_char(COALESCE(rec.created_at, NOW()), 'YYYY') || '-' || prefix || '-' || lpad(seq::text, 4, '0');
        END LOOP;
        UPDATE company SET license_id = new_id WHERE id = rec.id;
        seq := seq + 1;
    END LOOP;
END $$;

COMMENT ON COLUMN company.license_id IS 'Unique license identifier auto-generated on company creation. Format: QF-YYYY-XXX-0000. Never changes — used to identify the client subscription.';
