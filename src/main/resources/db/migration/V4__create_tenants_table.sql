-- Tenants table to track all tenants in the system
CREATE TABLE tenants (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Index for listing active tenants
CREATE INDEX idx_tenants_active ON tenants(active);
