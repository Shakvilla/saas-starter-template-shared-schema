package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.persistence.HibernateTenantFilterConfigurer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver = new TenantResolver();

    private final HibernateTenantFilterConfigurer filterConfigurer;

    public TenantFilter(HibernateTenantFilterConfigurer filterConfigurer){
        this.filterConfigurer = filterConfigurer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException{


        try{
            String tenantId = tenantResolver.resolveTenant(request);
            TenantContext.setTenantId(tenantId);
            filterConfigurer.enableTenantFilter();
            filterChain.doFilter(request, response);
        }finally{
            filterConfigurer.disableTenantFilter();
            TenantContext.clear();
        }

    }
}
