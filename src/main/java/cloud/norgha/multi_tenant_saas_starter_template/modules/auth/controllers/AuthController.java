package cloud.norgha.multi_tenant_saas_starter_template.modules.auth.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.AuthResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.LoginRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.RegisterRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.services.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 * These endpoints are publicly accessible (no authentication required).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication Management", description = "This group of endpoints are used to manage authentication operations")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user in the current tenant.
     * The first user in a tenant automatically becomes an ADMIN.
     *
     * @param request Registration details
     * @return JWT token response
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request Login credentials
     * @return JWT token response
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
