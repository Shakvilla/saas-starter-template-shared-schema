package cloud.norgha.multi_tenant_saas_starter_template.modules.users;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

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
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHarsh;


    @Column(nullable = false)
    private String role;

    protected User(){}

    public User(UUID id, String email, String passwordHarsh, String role) {
        this.id = id;
        this.email = email;
        this.passwordHarsh = passwordHarsh;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHarsh() {
        return passwordHarsh;
    }

    public String getRole() {
        return role;
    }
}
