package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for system admin details.
 */
public record AdminResponseDto(
        UUID id,
        String email,
        String fullName,
        boolean active,
        Instant createdAt
) {}
