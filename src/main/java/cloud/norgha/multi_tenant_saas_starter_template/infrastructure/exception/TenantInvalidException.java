package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception;

/**
 * Exception thrown when a tenant ID contains invalid characters.
 * Valid tenant IDs contain only alphanumeric characters, hyphens, and underscores.
 */
public class TenantInvalidException extends RuntimeException {

    public TenantInvalidException(String tenantId) {
        super("Tenant ID contains invalid characters: " + tenantId);
    }
}
