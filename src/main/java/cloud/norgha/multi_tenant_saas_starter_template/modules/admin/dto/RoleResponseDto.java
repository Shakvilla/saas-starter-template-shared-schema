package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for system role.
 */
public record RoleResponseDto(
        UUID id,
        String name,
        String description,
        Set<String> permissions,
        LocalDateTime createdAt
) {}
