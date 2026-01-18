package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

import java.time.Instant;

/**
 * Response DTO for tenant details.
 */
public record TenantResponseDto(
        String id,
        String name,
        boolean active,
        Instant createdAt
) {}
