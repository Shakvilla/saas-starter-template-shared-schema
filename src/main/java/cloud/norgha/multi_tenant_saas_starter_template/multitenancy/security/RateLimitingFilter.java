package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter for authentication endpoints.
 * Limits requests to prevent brute force attacks.
 * Includes scheduled cleanup to prevent memory leaks.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    // Rate limit: 10 requests per minute per IP
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute

    // Store request counts per IP
    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only rate limit auth endpoints
        return !path.startsWith("/api/v1/auth/") && !path.startsWith("/api/v1/admin/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        
        if (isRateLimited(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();
        
        RateLimitEntry entry = requestCounts.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_SIZE_MS) {
                // Start new window
                return new RateLimitEntry(now, new AtomicInteger(1));
            } else {
                // Increment counter in current window
                existing.count.incrementAndGet();
                return existing;
            }
        });

        return entry.count.get() > MAX_REQUESTS_PER_MINUTE;
    }

    /**
     * Extracts the client's IP address from the HTTP request.
     *
     * Prefers the first entry in the `X-Forwarded-For` header when present; otherwise uses the request's remote address.
     *
     * @param request the incoming HTTP servlet request
     * @return the client's IP address (first `X-Forwarded-For` value if available, otherwise `request.getRemoteAddr()`)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Removes stale per-IP rate limit entries to prevent memory growth.
     *
     * Runs every 5 minutes and deletes entries whose window start is older than twice the configured window size.
     * Logs the number of removed entries when any are cleaned up.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        int beforeSize = requestCounts.size();
        
        requestCounts.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart > WINDOW_SIZE_MS * 2);
        
        int removed = beforeSize - requestCounts.size();
        if (removed > 0) {
            log.debug("Cleaned up {} stale rate limit entries", removed);
        }
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        /**
         * Create a rate-limit entry for a client IP with the specified window start and request count.
         *
         * @param windowStart the start timestamp of the current rate-limit window, in milliseconds since the epoch
         * @param count an AtomicInteger tracking the number of requests observed in the current window
         */
        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
