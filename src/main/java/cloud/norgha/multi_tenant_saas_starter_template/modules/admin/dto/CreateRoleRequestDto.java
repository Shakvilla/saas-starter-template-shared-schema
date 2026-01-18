package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

/**
 * DTO for creating a new role.
 */
public record CreateRoleRequestDto(
        @NotBlank(message = "Role name is required")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Role name must be uppercase with underscores (e.g., MY_ROLE)")
        String name,

        String description,

        Set<String> permissionNames
) {}
