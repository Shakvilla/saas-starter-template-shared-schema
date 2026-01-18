package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for creating a new permission.
 */
public record CreatePermissionRequestDto(
        @NotBlank(message = "Permission name is required")
        @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "Permission name must be lowercase with underscores (e.g., my_permission)")
        String name,

        String description
) {}
