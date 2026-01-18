package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Tenant entity representing a tenant in the multi-tenant system.
 * This entity is NOT tenant-scoped - it tracks all tenants.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @NotBlank(message = "Tenant ID is required")
    private String id;

    @NotBlank(message = "Tenant name is required")
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Tenant() {}

    public Tenant(String id, String name) {
        this.id = id;
        this.name = name;
        this.active = true;
        this.createdAt = Instant.now();
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters

    public void setName(String name) {
        this.name = name;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
