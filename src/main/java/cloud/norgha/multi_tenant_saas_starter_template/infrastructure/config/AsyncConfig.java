package cloud.norgha.multi_tenant_saas_starter_template.infrastructure.config;

import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.async.TenantAwareTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async task execution with tenant context propagation.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configures the default async executor with tenant-aware task decoration.
     * This ensures @Async methods have access to the tenant context from the calling thread.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("Async-");
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();
        return executor;
    }
}
