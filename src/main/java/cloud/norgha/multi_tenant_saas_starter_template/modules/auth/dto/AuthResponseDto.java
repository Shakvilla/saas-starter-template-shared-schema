package cloud.norgha.multi_tenant_saas_starter_template.modules.auth.dto;

import java.time.Instant;

/**
 * Response DTO for authentication operations.
 * Returns the access token and its expiration.
 */
public record AuthResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt
) {
    /**
     * Creates an AuthResponseDto with Bearer token type.
     *
     * @param accessToken JWT access token
     * @param expiresIn   Token validity in seconds
     * @return AuthResponseDto instance
     */
    public static AuthResponseDto of(String accessToken, long expiresIn) {
        return new AuthResponseDto(
                accessToken,
                "Bearer",
                expiresIn,
                Instant.now().plusSeconds(expiresIn)
        );
    }
}
