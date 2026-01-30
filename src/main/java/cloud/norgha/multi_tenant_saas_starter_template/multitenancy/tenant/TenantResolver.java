package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant;

import cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception.TenantDeactivatedException;
import cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception.TenantMissingException;
import cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception.TenantNotFoundException;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.Tenant;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Resolves and validates tenant from incoming HTTP requests.
 * Validates that the tenant exists and is active before allowing the request to proceed.
 * Results are cached to reduce database lookups.
 */
@Component
public class TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(TenantResolver.class);
    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantRepository tenantRepository;

    public TenantResolver(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Resolves and validates the tenant from the request header.
     *
     * @param request The HTTP request containing the X-Tenant-ID header
     * @return The validated tenant ID
     * @throws TenantMissingException if the header is missing or blank
     * @throws TenantNotFoundException if the tenant does not exist
     * @throws TenantDeactivatedException if the tenant is deactivated
     */
    public String resolveTenant(HttpServletRequest request) {
        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantMissingException("Missing X-Tenant-ID header");
        }

//        sanitize tenant id input.
        if (!tenantId.matches("^[a-zA-Z0-9-_]+$")) {
            throw new RuntimeException(
                    "Tenant ID contains invalid characters"
            );
        }

        // Validate tenant using cached lookup
        validateTenant(tenantId);

        return tenantId.toLowerCase();
    }

    /**
     * Validates that a tenant exists and is active.
     * Results are cached to avoid repeated database lookups.
     *
     * @param tenantId The tenant ID to validate
     * @throws TenantNotFoundException if the tenant does not exist
     * @throws TenantDeactivatedException if the tenant is deactivated
     */
    @Cacheable(value = "tenants", key = "#tenantId")
    public Tenant validateTenant(String tenantId) {
        log.debug("Validating tenant (cache miss): {}", tenantId);
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (!tenant.isActive()) {
            throw new TenantDeactivatedException(tenantId);
        }

        return tenant;
    }
}
