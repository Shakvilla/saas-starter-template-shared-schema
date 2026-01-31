package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the application.
 * Uses Caffeine cache with TTL for tenant validation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TENANT_CACHE = "tenants";
    
    /**
     * TTL in minutes for cached tenant data.
     * After this time, tenant status will be re-fetched from database.
     */
    private static final int CACHE_TTL_MINUTES = 5;
    
    /**
     * Maximum number of cached entries to prevent memory leaks.
     */
    private static final int CACHE_MAX_SIZE = 1000;

    /**
     * Creates a Caffeine-based cache manager with TTL and size limits.
     * This ensures:
     * - Deactivated tenants are not served indefinitely from cache
     * - Memory usage is bounded
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(TENANT_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats());
        return cacheManager;
    }
}
