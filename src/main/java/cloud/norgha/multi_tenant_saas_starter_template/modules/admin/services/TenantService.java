package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateTenantRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.TenantResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.UpdateTenantRequestDto;

import java.util.List;

/**
 * Service interface for tenant management operations.
 */
public interface TenantService {

    /**
     * Creates a new tenant.
     *
     * @param request Tenant creation details
     * @return Created tenant response
     */
    TenantResponseDto createTenant(CreateTenantRequestDto request);

    /**
     * Gets all tenants.
     *
     * @return List of all tenants
     */
    List<TenantResponseDto> listTenants();

    /**
     * Gets a tenant by ID.
     *
     * @param id Tenant ID
     * @return Tenant response
     */
    TenantResponseDto getTenant(String id);

    /**
     * Updates a tenant.
     *
     * @param id      Tenant ID
     * @param request Update details
     * @return Updated tenant response
     */
    TenantResponseDto updateTenant(String id, UpdateTenantRequestDto request);

    /**
     * Deactivates a tenant.
     *
     * @param id Tenant ID
     */
    void deactivateTenant(String id);
}
