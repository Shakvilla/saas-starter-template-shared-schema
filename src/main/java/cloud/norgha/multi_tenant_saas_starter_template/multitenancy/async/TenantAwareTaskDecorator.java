package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.async;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantContext;
import org.springframework.core.task.TaskDecorator;

/**
 * Task decorator that captures and restores tenant context in async tasks.
 * Ensures that @Async methods have access to the tenant context from the calling thread.
 */
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture current tenant context
        String tenantId = TenantContext.getTenantId();
        
        return () -> {
            try {
                // Restore tenant context in async thread
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }
                runnable.run();
            } finally {
                // Clear tenant context after async execution
                TenantContext.clear();
            }
        };
    }
}
