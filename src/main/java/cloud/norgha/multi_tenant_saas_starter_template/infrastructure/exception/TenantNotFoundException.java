package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception;

/**
 * Exception thrown when a requested tenant does not exist in the system.
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }
}
