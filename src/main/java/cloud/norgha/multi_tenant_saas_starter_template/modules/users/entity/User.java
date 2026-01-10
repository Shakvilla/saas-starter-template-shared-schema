package cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name ="users")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tenantId;

    @NotNull(message = "Email is required!")
    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHarsh;


    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    protected User(){}

    public User(UUID id, String tenantId, String email, String passwordHarsh, String fullName, String role) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHarsh = passwordHarsh;
        this.fullName = fullName;
        this.role = role;
        this.active = true;
        this.createdAt = Instant.now();


    }

    public User(String tenantId, @Email(message = "Email type invalid") @NotBlank(message = "Email is required" ) String email, @Nullable String encode, @NotBlank(message = "Full Name is required") String passwordHarsh) {
    }

    public String getFullName() {
        return fullName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPasswordHarsh() {
        return passwordHarsh;
    }

    public String getRole() {
        return role;
    }
}
