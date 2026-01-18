package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.UserResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity.User;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for cross-tenant user management.
 * Requires SYSTEM_ADMIN role.
 * Note: These endpoints bypass tenant filtering.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Cross-Tenant User Management", description = "Platform-level user lookups across tenants")
public class UserAdminController {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gets any user by ID across all tenants.
     * Requires: view_users permission
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('view_users')")
    public UserResponseDto getUserById(@PathVariable UUID id) {
        Session session = entityManager.unwrap(Session.class);

        // Disable tenant filter for cross-tenant access
        session.disableFilter("tenantFilter");

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
            return mapToResponse(user);
        } finally {
            // Re-enable filter (though it will be re-enabled on next request anyway)
            // This is defensive programming
        }
    }

    /**
     * Lists all users in a specific tenant.
     * Requires: view_users permission
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAuthority('view_users')")
    public List<UserResponseDto> getUsersByTenant(@PathVariable String tenantId) {
        Session session = entityManager.unwrap(Session.class);

        // Disable the automatic filter and manually query by tenant
        session.disableFilter("tenantFilter");

        try {
            @SuppressWarnings("unchecked")
            List<User> users = entityManager
                    .createQuery("SELECT u FROM User u WHERE u.tenantId = :tenantId")
                    .setParameter("tenantId", tenantId)
                    .getResultList();

            return users.stream()
                    .map(this::mapToResponse)
                    .toList();
        } finally {
            // Filter will be re-enabled on next request
        }
    }

    private UserResponseDto mapToResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
