package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SystemAdmin entity.
 * Not tenant-filtered - provides access to all system admins.
 */
public interface SystemAdminRepository extends JpaRepository<SystemAdmin, UUID> {

    Optional<SystemAdmin> findByEmail(String email);

    boolean existsByEmail(String email);
}
