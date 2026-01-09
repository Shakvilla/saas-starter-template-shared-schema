package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence;


import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantContext;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantMissingException;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

@MappedSuperclass
public class BaseTenantEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    protected BaseTenantEntity(){}

    public String getTenantId() {
        return tenantId;
    }

    @PrePersist
    protected void assignTenant(){
        String currentTenant = TenantContext.getTenantId();

        if(currentTenant == null){
            throw new TenantMissingException("TenantContext not set before persisting entity: " + getClass().getSimpleName());


        }

        this.tenantId = currentTenant;
    }
}
