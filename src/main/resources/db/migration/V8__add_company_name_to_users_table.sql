ALTER TABLE users
    ADD COLUMN company_name VARCHAR(255);

-- Backfill existing rows safely
UPDATE users
SET company_name = full_name
WHERE company_name IS NULL;

-- Enforce constraint after backfill
ALTER TABLE users
    ALTER COLUMN company_name SET NOT NULL;

CREATE INDEX idx_users_company_name ON users(company_name);

