package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating a new tenant.
 */
public record CreateTenantRequestDto(
        @NotBlank(message = "Tenant ID is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Tenant ID must be lowercase alphanumeric with hyphens only")
        String id,

        @NotBlank(message = "Tenant name is required")
        String name
) {}
