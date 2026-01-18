package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for system permission.
 */
public record PermissionResponseDto(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt
) {}
