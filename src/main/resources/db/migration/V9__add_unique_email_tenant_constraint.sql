-- V9: Add unique constraint for email per tenant to prevent race conditions
-- This ensures database-level enforcement of email uniqueness within each tenant

ALTER TABLE users ADD CONSTRAINT uk_users_email_tenant UNIQUE (email, tenant_id);
