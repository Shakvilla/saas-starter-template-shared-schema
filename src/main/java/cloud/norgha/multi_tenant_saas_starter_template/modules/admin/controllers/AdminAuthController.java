package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.AdminLoginRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.AdminAuthService;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.AuthResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system admin authentication.
 * Does NOT require X-Tenant-ID header.
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "System Admin Authentication", description = "Platform admin authentication (no tenant required)")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * Authenticates a system admin and returns a JWT token.
     *
     * @param request Login credentials
     * @return JWT token response
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AdminLoginRequestDto request) {
        AuthResponseDto response = adminAuthService.login(request);
        return ResponseEntity.ok(response);
    }
}
