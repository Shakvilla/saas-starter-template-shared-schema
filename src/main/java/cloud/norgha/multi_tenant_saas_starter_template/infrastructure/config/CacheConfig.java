package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for the application.
 * Uses simple in-memory caching for tenant validation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TENANT_CACHE = "tenants";

    /**
     * Creates a simple cache manager using ConcurrentHashMap.
     * For production, consider using Caffeine or Redis for TTL support.
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(TENANT_CACHE);
    }
}
