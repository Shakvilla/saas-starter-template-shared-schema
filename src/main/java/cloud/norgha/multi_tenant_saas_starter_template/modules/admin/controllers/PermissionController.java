package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreatePermissionRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.PermissionResponseDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for system permission management.
 * Requires manage_admins permission.
 */
@RestController
@RequestMapping("/api/v1/admin/permissions")
@Tag(name = "Permission Management", description = "Create and manage system permissions")
public class PermissionController {

    private final RolePermissionService rolePermissionService;

    public PermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * Creates a new permission.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<PermissionResponseDto> createPermission(
            @Valid @RequestBody CreatePermissionRequestDto request
    ) {
        PermissionResponseDto response = rolePermissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all permissions.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<List<PermissionResponseDto>> listPermissions() {
        List<PermissionResponseDto> permissions = rolePermissionService.listPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * Gets a permission by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<PermissionResponseDto> getPermission(@PathVariable UUID id) {
        PermissionResponseDto permission = rolePermissionService.getPermission(id);
        return ResponseEntity.ok(permission);
    }

    /**
     * Deletes a permission.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        rolePermissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
