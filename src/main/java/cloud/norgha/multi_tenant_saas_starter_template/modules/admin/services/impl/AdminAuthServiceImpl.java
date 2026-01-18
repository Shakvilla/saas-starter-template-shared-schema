package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.impl;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.AdminLoginRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemAdmin;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.SystemAdminRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.AdminAuthService;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.AuthResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of AdminAuthService for system admin authentication.
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthServiceImpl.class);
    private static final String SYSTEM_TENANT = "SYSTEM";

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AdminAuthServiceImpl(
            SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(AdminLoginRequestDto request) {
        log.debug("Admin login attempt for email: {}", request.email());

        SystemAdmin admin = systemAdminRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email"));

        if (!admin.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Invalid password");
        }

        log.info("login password: {}", request.password());

        // Get roles from database
        Set<String> roleNames = admin.getRoleNames();
        Set<String> permissions = admin.getPermissions();

        log.info("Successful admin login for: {} with roles: {} and permissions: {}", 
                admin.getEmail(), roleNames, permissions);

        // Generate JWT with SYSTEM tenant, roles, and permissions
        String token = jwtTokenService.generateTokenWithPermissions(
                admin.getId().toString(),
                SYSTEM_TENANT,
                new ArrayList<>(roleNames),
                new ArrayList<>(permissions)
        );

        return AuthResponseDto.of(token, jwtTokenService.getExpirationSeconds());
    }
}
