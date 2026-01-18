package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception;

import java.time.Instant;

/**
 * Standardized API error response format.
 * Used by GlobalExceptionHandler to provide consistent error responses.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    /**
     * Creates an ApiErrorResponse with the current timestamp.
     *
     * @param status  HTTP status code
     * @param error   Error type (e.g., "Bad Request", "Unauthorized")
     * @param message Detailed error message
     * @param path    Request path that caused the error
     * @return ApiErrorResponse instance
     */
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path
        );
    }
}
