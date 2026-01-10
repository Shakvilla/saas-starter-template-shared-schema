package cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequestDto(

        @Email(message = "Email type invalid")
        @NotBlank(message = "Email is required" )
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Full Name is required")
        String fullName

        ) {
}
