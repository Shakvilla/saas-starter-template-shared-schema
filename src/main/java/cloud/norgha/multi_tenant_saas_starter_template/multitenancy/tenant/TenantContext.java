package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant;

/**
 * Thread-local storage for the current tenant context.
 * Uses InheritableThreadLocal to propagate tenant context to child threads (e.g., @Async methods).
 */
public final class TenantContext {

    private static final InheritableThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    private TenantContext() {}

    /**
     * Sets the current tenant ID for this thread and any child threads.
     *
     * @param tenantId The tenant ID to set
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the current tenant ID.
     *
     * @return The current tenant ID, or null if not set
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clears the current tenant context.
     * Should be called in a finally block after request processing.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
