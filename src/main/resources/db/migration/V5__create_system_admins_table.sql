-- System admins table for platform-level administrators
-- These users are NOT tenant-scoped and can manage all tenants
CREATE TABLE system_admins (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Index for email lookups during login
CREATE INDEX idx_system_admins_email ON system_admins(email);
