package cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto;

import cloud.norgha.multi_tenant_saas_starter_template.infrastructure.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user registration.
 */
public record RegisterRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Company name is required")
        String companyName
) {}

