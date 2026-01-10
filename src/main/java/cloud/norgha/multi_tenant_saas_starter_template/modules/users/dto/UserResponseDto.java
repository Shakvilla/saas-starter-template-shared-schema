package cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(UUID id, String email, String fullName, boolean active, Instant createdAt) {
}
