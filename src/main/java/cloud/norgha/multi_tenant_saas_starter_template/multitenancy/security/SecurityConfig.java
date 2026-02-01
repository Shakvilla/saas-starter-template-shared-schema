package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.HibernateTenantFilterConfigurer;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantFilter;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;


/**
 * Security configuration for the multi-tenant application.
 * Configures JWT-based stateless authentication with tenant isolation.
 * 
 * Routes:
 * - /auth/** : Tenant user authentication (requires X-Tenant-ID)
 * - /admin/auth/** : System admin authentication (no tenant required)
 * - /admin/** : System admin endpoints (requires SYSTEM_ADMIN role, no tenant)
 * - /** : Tenant-scoped endpoints (requires X-Tenant-ID + JWT)
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final Environment environment;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.swagger.server-url:http://localhost:8080}")
    private String swaggerServerUrl;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validates JWT secret configuration on startup.
     * Fails fast in production if using default/insecure secret.
     */
    @PostConstruct
    public void validateJwtSecret() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        if (jwtSecret.startsWith("default") || jwtSecret.length() < 32) {
            if (isProd) {
                throw new IllegalStateException(
                    "SECURITY ERROR: JWT_SECRET must be set to a secure value (min 32 chars) in production! " +
                    "Set the JWT_SECRET environment variable."
                );
            } else {
                log.warn("WARNING: Using default JWT secret. Set JWT_SECRET env var before deploying to production!");
            }
        }
    }

    /**
     * Creates the JwtTokenService bean with properly derived SecretKey.
     */
    @Bean
    public JwtTokenService jwtTokenService() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return new JwtTokenService(key, jwtExpiration);
    }

    /**
     * Creates the JwtAuthenticationFilter bean for tenant-scoped requests.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    /**
     * Creates the AdminJwtAuthenticationFilter bean for admin requests.
     * This filter does NOT validate tenant context.
     */
    @Bean
    public AdminJwtAuthenticationFilter adminJwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new AdminJwtAuthenticationFilter(jwtTokenService);
    }

    /**
     * Configures the security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RateLimitingFilter rateLimitingFilter,
            TenantFilter tenantFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AdminJwtAuthenticationFilter adminJwtAuthenticationFilter,
            org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Disable CSRF - not needed for stateless JWT auth
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger UI endpoints
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        .requestMatchers("/swagger-ui/api-docs/**").permitAll()
                        // Admin endpoints require SYSTEM_ADMIN role
                        .requestMatchers("/api/v1/admin/**").hasRole("SYSTEM_ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated())

                // RateLimitingFilter for auth endpoints (first filter)
                .addFilterBefore(
                        rateLimitingFilter,
                        UsernamePasswordAuthenticationFilter.class)

                // TenantFilter for tenant-scoped requests (skips /admin/**)
                .addFilterAfter(
                        tenantFilter,
                        RateLimitingFilter.class)

                // AdminJwtAuthenticationFilter for /admin/** requests
                .addFilterAfter(
                        adminJwtAuthenticationFilter,
                        TenantFilter.class)

                // JwtAuthenticationFilter for tenant-scoped requests
                .addFilterAfter(
                        jwtAuthenticationFilter,
                        AdminJwtAuthenticationFilter.class);

        return http.build();
    }



    @Bean
	public OpenAPI openAPI() {
		
		Server server = new Server();
		server.setUrl(swaggerServerUrl);
	    
	    return new OpenAPI().addSecurityItem(new SecurityRequirement().
	            addList("Bearer Authentication"))
	        .components(new Components().addSecuritySchemes
	            ("Bearer Authentication", createAPIKeyScheme()))
	        .info(new Info().title("Tenant Multi-Tenant SaaS Starter Template")
	            .description("The Tenant Multi-Tenant SaaS Starter Template.")
	            .version("1.0").contact(new Contact().name("Norgha")
	                .email( "info@norgha.cloud").url("www.norgha.cloud"))
	            .license(new License().name("Copyright. No part of this application shall be copied or reproduced without permission")
	                .url("www.norgha.cloud/licese.html"))
	            ).servers(List.of(server))
	        ;
	    
	}


    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
