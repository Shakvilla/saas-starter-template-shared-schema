CREATE TABLE IF NOT EXISTS users (
    id UUID  DEFAULT gen_random_uuid() PRIMARY KEY ,
    tenant_id VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    UNIQUE (tenant_id, email)

);

CREATE INDEX idx_users_tenant ON users (tenant_id);