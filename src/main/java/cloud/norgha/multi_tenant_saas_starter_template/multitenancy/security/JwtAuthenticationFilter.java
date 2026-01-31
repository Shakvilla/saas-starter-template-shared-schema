package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication filter that validates Bearer tokens and sets up the SecurityContext.
 * Runs after TenantFilter to ensure tenant context is available for tenant validation.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // No token provided - let the request continue (might be a public endpoint)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Jws<Claims> parsed = tokenService.parseAndValidate(token);
            Claims claims = parsed.getPayload();

            // Extract tenant from token and validate against request tenant
            String tokenTenant = claims.get("tenant", String.class);
            String requestTenant = TenantContext.getTenantId();

            // Validate tenant context is available
            if (requestTenant == null) {
                log.warn("JWT authentication attempted without tenant context");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-ID header");
                return;
            }

            // Validate tenant matches (case-insensitive since TenantResolver normalizes to lowercase)
            if (!tokenTenant.equalsIgnoreCase(requestTenant)) {
                log.warn("Tenant mismatch: token={}, request={}", tokenTenant, requestTenant);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant mismatch between token and request");
                return;
            }

            // Extract user info and roles
            String userId = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList()
            );

            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user: {} with roles: {}", userId, roles);

        } catch (Exception ex) {
            log.warn("JWT authentication failed: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
