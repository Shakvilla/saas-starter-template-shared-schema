package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.HibernateTenantFilterConfigurer;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantFilter;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Security configuration for the multi-tenant application.
 * Configures JWT-based stateless authentication with tenant isolation.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Creates the JwtTokenService bean with properly derived SecretKey.
     */
    @Bean
    public JwtTokenService jwtTokenService() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return new JwtTokenService(key, jwtExpiration);
    }

    /**
     * Creates the JwtAuthenticationFilter bean.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    /**
     * Configures the security filter chain.
     * - CSRF disabled (stateless JWT auth)
     * - Stateless session management
     * - Public access to /auth/** endpoints
     * - All other requests require authentication
     * - TenantFilter runs first to extract tenant from header
     * - JwtAuthenticationFilter validates JWT tokens
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HibernateTenantFilterConfigurer filterConfigurer,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
                // Disable CSRF - not needed for stateless JWT auth
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())

                // Add TenantFilter before authentication
                .addFilterBefore(
                        new TenantFilter(filterConfigurer),
                        UsernamePasswordAuthenticationFilter.class)

                // Add JwtAuthenticationFilter after TenantFilter
                .addFilterAfter(
                        jwtAuthenticationFilter,
                        TenantFilter.class);

        return http.build();
    }
}
