package cloud.norgha.multi_tenant_saas_starter_template.multitenancy;

import cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception.TenantInvalidException;
import cloud.norgha.multi_tenant_saas_starter_template.infrastructure.exception.TenantMissingException;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.async.TenantAwareTaskDecorator;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantContext;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantResolver;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for multi-tenancy components.
 * Tests tenant context management, resolver validation, and async context propagation.
 */
@ExtendWith(MockitoExtension.class)
class MultiTenancySecurityTest {

    @Nested
    @DisplayName("TenantContext Tests")
    class TenantContextTests {

        @AfterEach
        void cleanup() {
            TenantContext.clear();
        }

        @Test
        @DisplayName("should set and get tenant ID")
        void shouldSetAndGetTenantId() {
            TenantContext.setTenantId("test-tenant");
            assertThat(TenantContext.getTenantId()).isEqualTo("test-tenant");
        }

        @Test
        @DisplayName("should return null when tenant not set")
        void shouldReturnNullWhenNotSet() {
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should clear tenant context")
        void shouldClearContext() {
            TenantContext.setTenantId("test-tenant");
            TenantContext.clear();
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("requireTenantId should return tenant when set")
        void requireTenantIdShouldReturn() {
            TenantContext.setTenantId("test-tenant");
            assertThat(TenantContext.requireTenantId()).isEqualTo("test-tenant");
        }

        @Test
        @DisplayName("requireTenantId should throw when not set")
        void requireTenantIdShouldThrow() {
            assertThatThrownBy(TenantContext::requireTenantId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Tenant context not set");
        }

        @Test
        @DisplayName("should propagate to child thread via InheritableThreadLocal")
        void shouldPropagateToChildThread() throws InterruptedException {
            TenantContext.setTenantId("parent-tenant");
            
            AtomicReference<String> childTenant = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            Thread child = new Thread(() -> {
                childTenant.set(TenantContext.getTenantId());
                latch.countDown();
            });
            child.start();
            
            latch.await(1, TimeUnit.SECONDS);
            assertThat(childTenant.get()).isEqualTo("parent-tenant");
        }
    }

    @Nested
    @DisplayName("TenantResolver Tests")
    class TenantResolverTests {

        @Mock
        private TenantRepository tenantRepository;

        private TenantResolver resolver;

        @BeforeEach
        void setUp() {
            resolver = new TenantResolver(tenantRepository);
        }

        @Test
        @DisplayName("should throw TenantMissingException when header is missing")
        void shouldThrowWhenHeaderMissing() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            
            assertThatThrownBy(() -> resolver.resolveTenant(request))
                    .isInstanceOf(TenantMissingException.class)
                    .hasMessageContaining("Missing X-Tenant-ID header");
        }

        @Test
        @DisplayName("should throw TenantMissingException when header is blank")
        void shouldThrowWhenHeaderBlank() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-ID", "   ");
            
            assertThatThrownBy(() -> resolver.resolveTenant(request))
                    .isInstanceOf(TenantMissingException.class);
        }

        @Test
        @DisplayName("should throw TenantInvalidException for invalid characters")
        void shouldThrowForInvalidCharacters() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-ID", "tenant<script>");
            
            assertThatThrownBy(() -> resolver.resolveTenant(request))
                    .isInstanceOf(TenantInvalidException.class)
                    .hasMessageContaining("invalid characters");
        }

        @Test
        @DisplayName("should throw TenantInvalidException for SQL injection attempt")
        void shouldThrowForSqlInjection() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-ID", "tenant'; DROP TABLE users;--");
            
            assertThatThrownBy(() -> resolver.resolveTenant(request))
                    .isInstanceOf(TenantInvalidException.class);
        }

        @Test
        @DisplayName("should accept valid tenant ID with alphanumeric, hyphens, underscores")
        void shouldAcceptValidTenantId() {
            // Note: This test would need mocked tenantRepository.findById to fully pass
            // For now, we verify the validation passes before the DB lookup throws
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-ID", "Valid-Tenant_123");
            
            // Will throw TenantNotFoundException because mock returns empty, 
            // but that proves validation passed
            assertThatThrownBy(() -> resolver.resolveTenant(request))
                    .isNotInstanceOf(TenantInvalidException.class)
                    .isNotInstanceOf(TenantMissingException.class);
        }
    }

    @Nested
    @DisplayName("TenantAwareTaskDecorator Tests")
    class TenantAwareTaskDecoratorTests {

        private TenantAwareTaskDecorator decorator;

        @BeforeEach
        void setUp() {
            decorator = new TenantAwareTaskDecorator();
        }

        @AfterEach
        void cleanup() {
            TenantContext.clear();
        }

        @Test
        @DisplayName("should propagate tenant context to decorated task")
        void shouldPropagateContext() throws InterruptedException {
            TenantContext.setTenantId("async-tenant");
            
            AtomicReference<String> capturedTenant = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            Runnable originalTask = () -> {
                capturedTenant.set(TenantContext.getTenantId());
                latch.countDown();
            };
            
            Runnable decoratedTask = decorator.decorate(originalTask);
            
            // Clear context to simulate ThreadPoolExecutor that reuses threads
            TenantContext.clear();
            
            // Run in new thread (simulating async executor)
            new Thread(decoratedTask).start();
            
            latch.await(1, TimeUnit.SECONDS);
            assertThat(capturedTenant.get()).isEqualTo("async-tenant");
        }

        @Test
        @DisplayName("should clear tenant context after task completion")
        void shouldClearContextAfterTask() throws InterruptedException {
            TenantContext.setTenantId("temp-tenant");
            
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> tenantAfterTask = new AtomicReference<>();
            
            Runnable originalTask = () -> {
                // Task runs here
            };
            
            Runnable decoratedTask = decorator.decorate(originalTask);
            
            Thread asyncThread = new Thread(() -> {
                decoratedTask.run();
                tenantAfterTask.set(TenantContext.getTenantId());
                latch.countDown();
            });
            
            asyncThread.start();
            latch.await(1, TimeUnit.SECONDS);
            
            // After decorated task completes, context should be cleared in that thread
            assertThat(tenantAfterTask.get()).isNull();
        }

        @Test
        @DisplayName("should handle null tenant context gracefully")
        void shouldHandleNullContext() throws InterruptedException {
            // Don't set tenant context
            
            AtomicReference<String> capturedTenant = new AtomicReference<>("initial");
            CountDownLatch latch = new CountDownLatch(1);
            
            Runnable originalTask = () -> {
                capturedTenant.set(TenantContext.getTenantId());
                latch.countDown();
            };
            
            Runnable decoratedTask = decorator.decorate(originalTask);
            new Thread(decoratedTask).start();
            
            latch.await(1, TimeUnit.SECONDS);
            assertThat(capturedTenant.get()).isNull();
        }
    }
}
