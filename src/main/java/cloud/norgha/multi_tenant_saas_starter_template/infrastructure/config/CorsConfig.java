package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration for cross-origin requests.
 * Configurable via application properties for different environments.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    /**
     * Provide a CorsConfigurationSource that applies the configured CORS rules to API endpoints.
     *
     * @return a CorsConfigurationSource that registers CORS settings (including allowed origins, methods,
     *         headers, credentials flag, max age, and exposed headers "Authorization" and "X-Tenant-ID")
     *         for the path pattern "/api/**"
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);
        
        // Expose headers for JWT token handling
        config.setExposedHeaders(List.of("Authorization", "X-Tenant-ID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}