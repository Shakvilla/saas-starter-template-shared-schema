package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemRoleRepository extends JpaRepository<SystemRole, UUID> {
    Optional<SystemRole> findByName(String name);
}
