package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.AdminLoginRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.AuthResponseDto;

/**
 * Service interface for system admin authentication.
 */
public interface AdminAuthService {

    /**
     * Authenticates a system admin and returns a JWT token.
     *
     * @param request Login credentials
     * @return Authentication response with JWT token
     */
    AuthResponseDto login(AdminLoginRequestDto request);
}
