package cloud.norgha.multi_tenant_saas_starter_template.modules.users.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.UpdateUserRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.UserResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user management endpoints.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gets the currently authenticated user's profile.
     *
     * @return Current user details
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        UserResponseDto user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    /**
     * Lists all users in the current tenant.
     * Requires ADMIN role.
     *
     * @return List of all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDto>> listUsers() {
        List<UserResponseDto> users = userService.listUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Gets a specific user by ID.
     * Requires ADMIN role.
     *
     * @param id User ID
     * @return User details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Updates a user's profile.
     * Requires ADMIN role.
     *
     * @param id      User ID to update
     * @param request Update details
     * @return Updated user details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequestDto request
    ) {
        UserResponseDto user = userService.updateUser(id, request.fullName(), request.email());
        return ResponseEntity.ok(user);
    }

    /**
     * Deactivates a user (soft delete).
     * Requires ADMIN role.
     *
     * @param id User ID to deactivate
     * @return No content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}
