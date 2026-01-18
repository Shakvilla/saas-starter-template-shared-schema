package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.AdminResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateAdminRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.SystemAdminService;
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
 * REST controller for system admin management.
 * Requires manage_admins permission.
 */
@RestController
@RequestMapping("/api/v1/admin/system-admins")
@Tag(name = "System Admin Management", description = "Create and manage platform administrators")
public class SystemAdminController {

    private final SystemAdminService adminService;

    public SystemAdminController(SystemAdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Creates a new system admin.
     * Requires: manage_admins permission
     */
    @PostMapping
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<AdminResponseDto> createAdmin(@Valid @RequestBody CreateAdminRequestDto request) {
        AdminResponseDto response = adminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all system admins.
     * Requires: manage_admins permission
     */
    @GetMapping
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<List<AdminResponseDto>> listAdmins() {
        List<AdminResponseDto> admins = adminService.listAdmins();
        return ResponseEntity.ok(admins);
    }

    /**
     * Gets a system admin by ID.
     * Requires: manage_admins permission
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<AdminResponseDto> getAdmin(@PathVariable UUID id) {
        AdminResponseDto admin = adminService.getAdmin(id);
        return ResponseEntity.ok(admin);
    }

    /**
     * Deactivates a system admin.
     * Requires: manage_admins permission
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_admins')")
    public ResponseEntity<Void> deactivateAdmin(@PathVariable UUID id) {
        adminService.deactivateAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
