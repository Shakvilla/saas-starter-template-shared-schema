package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.AdminResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateAdminRequestDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing system admins.
 */
public interface SystemAdminService {
    
    /**
     * Creates a new system admin.
     */
    AdminResponseDto createAdmin(CreateAdminRequestDto request);
    
    /**
     * Lists all system admins.
     */
    List<AdminResponseDto> listAdmins();
    
    /**
     * Gets an admin by ID.
     */
    AdminResponseDto getAdmin(UUID id);
    
    /**
     * Deactivates an admin.
     */
    void deactivateAdmin(UUID id);
}
