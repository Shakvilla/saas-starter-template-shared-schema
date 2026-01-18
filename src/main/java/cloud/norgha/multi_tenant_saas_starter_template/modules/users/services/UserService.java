package cloud.norgha.multi_tenant_saas_starter_template.modules.users.services;

import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.CreateUserRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.UserResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    /**
     * Creates a new user in the current tenant.
     *
     * @param request User creation request with email, password, and full name
     * @return Created user response
     */
    UserResponseDto createUser(CreateUserRequestDto request);

    /**
     * Gets the currently authenticated user.
     *
     * @return Current user response
     */
    UserResponseDto getCurrentUser();

    /**
     * Lists all users in the current tenant.
     *
     * @return List of user responses
     */
    List<UserResponseDto> listUsers();

    /**
     * Finds a user by email within the current tenant.
     *
     * @param email Email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Gets a user by their ID.
     *
     * @param id User ID
     * @return User response
     */
    UserResponseDto getUserById(UUID id);

    /**
     * Updates a user's profile.
     *
     * @param id       User ID
     * @param fullName New full name (null to keep existing)
     * @param email    New email (null to keep existing)
     * @return Updated user response
     */
    UserResponseDto updateUser(UUID id, String fullName, String email);

    /**
     * Deactivates a user (soft delete).
     *
     * @param id User ID to deactivate
     */
    void deactivateUser(UUID id);
}
