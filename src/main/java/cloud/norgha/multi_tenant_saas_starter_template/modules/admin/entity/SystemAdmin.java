package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * System administrator entity for platform-level access.
 * NOT tenant-scoped - can manage all tenants.
 */
@Entity
@Table(name = "system_admins")
public class SystemAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "Full name is required")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "system_admin_roles",
            joinColumns = @JoinColumn(name = "admin_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<SystemRole> roles = new HashSet<>();

    protected SystemAdmin() {}

    public SystemAdmin(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = true;
        this.createdAt = Instant.now();
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<SystemRole> getRoles() {
        return roles;
    }

    /**
     * Gets all role names for this admin.
     */
    public Set<String> getRoleNames() {
        return roles.stream()
                .map(SystemRole::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all permissions from all roles (flattened).
     */
    public Set<String> getPermissions() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(SystemPermission::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if admin has a specific permission.
     */
    public boolean hasPermission(String permissionName) {
        return getPermissions().contains(permissionName);
    }

    /**
     * Checks if admin has a specific role.
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    // Setters

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setRoles(Set<SystemRole> roles) {
        this.roles = roles;
    }

    public void addRole(SystemRole role) {
        this.roles.add(role);
    }

    public void removeRole(SystemRole role) {
        this.roles.remove(role);
    }
}
