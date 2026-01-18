package cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto;

import jakarta.validation.constraints.Email;

/**
 * Request DTO for updating user profile.
 */
public record UpdateUserRequestDto(
        String fullName,

        @Email(message = "Email format is invalid")
        String email
) {}
