package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for Tenant entity.
 * Not tenant-filtered - provides access to all tenants.
 */
public interface TenantRepository extends JpaRepository<Tenant, String> {

    List<Tenant> findByActiveTrue();

    boolean existsById(String id);
}
