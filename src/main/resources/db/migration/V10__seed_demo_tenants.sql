-- Seed data for development and testing
-- Creates a demo tenant for testing multi-tenancy features
INSERT INTO tenants (id, name, active)
VALUES ('demo', 'Demo Tenant', true)
ON CONFLICT (id) DO NOTHING;

-- Also create a 'tenant1' for backward compatibility with tests
INSERT INTO tenants (id, name, active)
VALUES ('tenant1', 'Test Tenant 1', true)
ON CONFLICT (id) DO NOTHING;
