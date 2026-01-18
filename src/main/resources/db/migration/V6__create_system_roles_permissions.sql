-- =====================================================
-- V6: Create System Roles and Permissions tables
-- =====================================================

-- System roles (e.g., SUPER_ADMIN, SUPPORT_ADMIN, BILLING_ADMIN)
CREATE TABLE system_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- System permissions (granular access controls)
CREATE TABLE system_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Role-Permission mapping (many-to-many)
CREATE TABLE system_role_permissions (
    role_id UUID NOT NULL REFERENCES system_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES system_permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Admin-Role mapping (many-to-many)
CREATE TABLE system_admin_roles (
    admin_id UUID NOT NULL REFERENCES system_admins(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES system_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (admin_id, role_id)
);

-- Indexes for performance
CREATE INDEX idx_system_role_permissions_role ON system_role_permissions(role_id);
CREATE INDEX idx_system_role_permissions_permission ON system_role_permissions(permission_id);
CREATE INDEX idx_system_admin_roles_admin ON system_admin_roles(admin_id);
CREATE INDEX idx_system_admin_roles_role ON system_admin_roles(role_id);
