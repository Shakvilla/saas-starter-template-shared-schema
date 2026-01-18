package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreatePermissionRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateRoleRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.PermissionResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.RoleResponseDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing system roles and permissions.
 */
public interface RolePermissionService {

    // === Role Operations ===
    
    RoleResponseDto createRole(CreateRoleRequestDto request);
    
    List<RoleResponseDto> listRoles();
    
    RoleResponseDto getRole(UUID id);
    
    RoleResponseDto updateRolePermissions(UUID roleId, Set<String> permissionNames);
    
    void deleteRole(UUID id);

    // === Permission Operations ===
    
    PermissionResponseDto createPermission(CreatePermissionRequestDto request);
    
    List<PermissionResponseDto> listPermissions();
    
    PermissionResponseDto getPermission(UUID id);
    
    void deletePermission(UUID id);

    // === Admin Role Assignment ===
    
    void assignRoleToAdmin(UUID adminId, String roleName);
    
    void removeRoleFromAdmin(UUID adminId, String roleName);
}
