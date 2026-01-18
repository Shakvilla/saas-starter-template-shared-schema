package cloud.norgha.multi_tenant_saas_starter_template.modules.auth.services.impl;

import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.AuthResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.LoginRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto.RegisterRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.auth.services.AuthService;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity.User;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.repository.UserRepository;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security.JwtTokenService;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of AuthService for handling authentication and registration.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Registering user with email: {} in tenant: {}", request.email(), tenantId);

        // Check if email already exists in this tenant
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered in this tenant");
        }

        // First user in tenant becomes ADMIN
        boolean isFirstUser = userRepository.count() == 0;
        String role = isFirstUser ? ROLE_ADMIN : ROLE_USER;

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                role
        );

        User saved = userRepository.save(user);
        log.info("Registered user with ID: {} and role: {} in tenant: {}", saved.getId(), role, tenantId);

        // Generate JWT token
        String token = jwtTokenService.generateToken(
                saved.getId().toString(),
                tenantId,
                List.of(role)
        );

        return AuthResponseDto.of(token, jwtTokenService.getExpirationSeconds());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String tenantId = TenantContext.getTenantId();
        log.debug("Login attempt for email: {} in tenant: {}", request.email(), tenantId);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("Successful login for user ID: {} in tenant: {}", user.getId(), tenantId);

        // Generate JWT token
        String token = jwtTokenService.generateToken(
                user.getId().toString(),
                tenantId,
                List.of(user.getRole())
        );

        return AuthResponseDto.of(token, jwtTokenService.getExpirationSeconds());
    }
}
