package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateRoleRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.RoleResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.RolePermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for system role management.
 * Requires manage_admins permission.
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@Tag(name = "Role Management", description = "Create and manage system roles")
public class RoleController {

    private final RolePermissionService rolePermissionService;

    public RoleController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * Creates a new role.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<RoleResponseDto> createRole(@Valid @RequestBody CreateRoleRequestDto request) {
        RoleResponseDto response = rolePermissionService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all roles.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<List<RoleResponseDto>> listRoles() {
        List<RoleResponseDto> roles = rolePermissionService.listRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Gets a role by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<RoleResponseDto> getRole(@PathVariable UUID id) {
        RoleResponseDto role = rolePermissionService.getRole(id);
        return ResponseEntity.ok(role);
    }

    /**
     * Updates a role's permissions.
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<RoleResponseDto> updateRolePermissions(
            @PathVariable UUID id,
            @RequestBody Set<String> permissionNames
    ) {
        RoleResponseDto role = rolePermissionService.updateRolePermissions(id, permissionNames);
        return ResponseEntity.ok(role);
    }

    /**
     * Deletes a role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        rolePermissionService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigns a role to an admin.
     */
    @PostMapping("/{roleName}/admins/{adminId}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<Void> assignRoleToAdmin(
            @PathVariable String roleName,
            @PathVariable UUID adminId
    ) {
        rolePermissionService.assignRoleToAdmin(adminId, roleName);
        return ResponseEntity.ok().build();
    }

    /**
     * Removes a role from an admin.
     */
    @DeleteMapping("/{roleName}/admins/{adminId}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<Void> removeRoleFromAdmin(
            @PathVariable String roleName,
            @PathVariable UUID adminId
    ) {
        rolePermissionService.removeRoleFromAdmin(adminId, roleName);
        return ResponseEntity.noContent().build();
    }
}
