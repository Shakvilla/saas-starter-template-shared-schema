package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception;

/**
 * Exception thrown when a tenant exists but is deactivated.
 */
public class TenantDeactivatedException extends RuntimeException {

    public TenantDeactivatedException(String tenantId) {
        super("Tenant is deactivated: " + tenantId);
    }
}
