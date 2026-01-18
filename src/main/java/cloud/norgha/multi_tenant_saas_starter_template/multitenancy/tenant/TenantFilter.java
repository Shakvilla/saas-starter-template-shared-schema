package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.HibernateTenantFilterConfigurer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts tenant ID from request header and sets up tenant context.
 * Skips /admin/** paths as they don't require tenant context.
 */
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    private final TenantResolver tenantResolver = new TenantResolver();
    private final HibernateTenantFilterConfigurer filterConfigurer;

    public TenantFilter(HibernateTenantFilterConfigurer filterConfigurer) {
        this.filterConfigurer = filterConfigurer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip tenant resolution for admin and swagger paths
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/admin/") 
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String tenantId = tenantResolver.resolveTenant(request);
            TenantContext.setTenantId(tenantId);
            filterConfigurer.enableTenantFilter();
            log.debug("Tenant context set: {}", tenantId);
            filterChain.doFilter(request, response);
        } finally {
            filterConfigurer.disableTenantFilter();
            TenantContext.clear();
        }
    }
}
