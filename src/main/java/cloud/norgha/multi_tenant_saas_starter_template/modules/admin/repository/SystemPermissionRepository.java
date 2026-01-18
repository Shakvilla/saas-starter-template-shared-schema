package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemPermissionRepository extends JpaRepository<SystemPermission, UUID> {
    Optional<SystemPermission> findByName(String name);
    boolean existsByName(String name);
}
