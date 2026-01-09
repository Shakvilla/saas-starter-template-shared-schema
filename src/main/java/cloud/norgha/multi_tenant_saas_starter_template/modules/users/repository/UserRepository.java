package cloud.norgha.multi_tenant_saas_starter_template.modules.users.repository;

import cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository  extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
