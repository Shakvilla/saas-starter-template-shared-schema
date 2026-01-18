package cloud.norgha.multi_tenant_saas_starter_template.modules.auth.services;

import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.AuthResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.LoginRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.RegisterRequestDto;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user in the current tenant.
     * The first user in a tenant automatically becomes an ADMIN.
     *
     * @param request Registration request with email, password, and full name
     * @return Authentication response with JWT token
     */
    AuthResponseDto register(RegisterRequestDto request);

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request Login request with email and password
     * @return Authentication response with JWT token
     */
    AuthResponseDto login(LoginRequestDto request);
}
