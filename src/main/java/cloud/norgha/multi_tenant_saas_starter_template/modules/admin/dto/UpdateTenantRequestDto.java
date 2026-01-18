package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

/**
 * Request DTO for updating a tenant.
 */
public record UpdateTenantRequestDto(
        String name,
        Boolean active
) {}
