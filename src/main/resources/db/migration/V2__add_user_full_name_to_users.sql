ALTER TABLE users
    ADD COLUMN full_name VARCHAR(255);

-- Backfill existing rows safely
UPDATE users
SET full_name = email
WHERE full_name IS NULL;

-- Enforce constraint after backfill
ALTER TABLE users
    ALTER COLUMN full_name SET NOT NULL;
