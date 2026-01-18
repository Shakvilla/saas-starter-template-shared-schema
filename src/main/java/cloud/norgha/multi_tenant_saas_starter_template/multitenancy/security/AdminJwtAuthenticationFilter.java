package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;

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
 * JWT Authentication filter for admin routes.
 * Unlike JwtAuthenticationFilter, this does NOT validate tenant context.
 * Only processes requests to /admin/** paths.
 */
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminJwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;

    public AdminJwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only process /api/v1/admin/** paths (but not /api/v1/admin/auth/**)
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/admin/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // No token provided
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Jws<Claims> parsed = tokenService.parseAndValidate(token);
            Claims claims = parsed.getPayload();

            // Extract user info, roles, and permissions (NO tenant validation for admin)
            String userId = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);

            // Build authorities from roles (ROLE_X) and permissions
            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            
            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
            
            if (permissions != null) {
                permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
            }

            // Create authentication token with combined authorities
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Admin authenticated: {} with roles: {} and permissions: {}", userId, roles, permissions);

        } catch (Exception ex) {
            log.warn("Admin JWT authentication failed: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
