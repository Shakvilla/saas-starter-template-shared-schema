package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.controllers;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateTenantRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.TenantResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.UpdateTenantRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.TenantService;
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

/**
 * REST controller for tenant management.
 * Requires SYSTEM_ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin/tenants")
@Tag(name = "Tenant Management", description = "Platform-level tenant CRUD operations")
public class TenantAdminController {

    private final TenantService tenantService;

    public TenantAdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Creates a new tenant.
     * Requires: manage_tenants permission
     */
    @PostMapping
    @PreAuthorize("hasAuthority('manage_tenants')")
    public ResponseEntity<TenantResponseDto> createTenant(@Valid @RequestBody CreateTenantRequestDto request) {
        TenantResponseDto response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all tenants.
     * Requires: view_tenants permission
     */
    @GetMapping
    @PreAuthorize("hasAuthority('view_tenants')")
    public ResponseEntity<List<TenantResponseDto>> listTenants() {
        List<TenantResponseDto> tenants = tenantService.listTenants();
        return ResponseEntity.ok(tenants);
    }

    /**
     * Gets a tenant by ID.
     * Requires: view_tenants permission
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('view_tenants')")
    public ResponseEntity<TenantResponseDto> getTenant(@PathVariable String id) {
        TenantResponseDto tenant = tenantService.getTenant(id);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Updates a tenant.
     * Requires: manage_tenants permission
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_tenants')")
    public ResponseEntity<TenantResponseDto> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequestDto request
    ) {
        TenantResponseDto tenant = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Deactivates a tenant.
     * Requires: manage_tenants permission
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_tenants')")
    public ResponseEntity<Void> deactivateTenant(@PathVariable String id) {
        tenantService.deactivateTenant(id);
        return ResponseEntity.noContent().build();
    }
}
