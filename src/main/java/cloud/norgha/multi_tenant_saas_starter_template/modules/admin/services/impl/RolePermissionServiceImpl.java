package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.impl;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreatePermissionRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateRoleRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.PermissionResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.RoleResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemAdmin;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemPermission;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemRole;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.SystemAdminRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.SystemPermissionRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.SystemRoleRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.RolePermissionService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionServiceImpl.class);

    private final SystemRoleRepository roleRepository;
    private final SystemPermissionRepository permissionRepository;
    private final SystemAdminRepository adminRepository;

    public RolePermissionServiceImpl(
            SystemRoleRepository roleRepository,
            SystemPermissionRepository permissionRepository,
            SystemAdminRepository adminRepository
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.adminRepository = adminRepository;
    }

    // === Role Operations ===

    @Override
    @Transactional
    public RoleResponseDto createRole(CreateRoleRequestDto request) {
        if (roleRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + request.name());
        }

        SystemRole role = new SystemRole(request.name(), request.description());

        // Assign permissions if provided
        if (request.permissionNames() != null && !request.permissionNames().isEmpty()) {
            Set<SystemPermission> permissions = resolvePermissions(request.permissionNames());
            role.setPermissions(permissions);
        }

        SystemRole saved = roleRepository.save(role);
        log.info("Created role: {} with permissions: {}", saved.getName(), 
                saved.getPermissions().stream().map(SystemPermission::getName).toList());

        return mapToRoleResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDto> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToRoleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponseDto getRole(UUID id) {
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
        return mapToRoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponseDto updateRolePermissions(UUID roleId, Set<String> permissionNames) {
        SystemRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        Set<SystemPermission> permissions = resolvePermissions(permissionNames);
        role.setPermissions(permissions);

        SystemRole saved = roleRepository.save(role);
        log.info("Updated permissions for role {}: {}", saved.getName(), permissionNames);

        return mapToRoleResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRole(UUID id) {
        SystemRole role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
        
        // Prevent deletion of built-in roles
        if (role.getName().equals("SUPER_ADMIN")) {
            throw new IllegalArgumentException("Cannot delete built-in SUPER_ADMIN role");
        }
        
        roleRepository.delete(role);
        log.info("Deleted role: {}", role.getName());
    }

    // === Permission Operations ===

    @Override
    @Transactional
    public PermissionResponseDto createPermission(CreatePermissionRequestDto request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Permission already exists: " + request.name());
        }

        SystemPermission permission = new SystemPermission(request.name(), request.description());
        SystemPermission saved = permissionRepository.save(permission);
        log.info("Created permission: {}", saved.getName());

        return mapToPermissionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponseDto> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToPermissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponseDto getPermission(UUID id) {
        SystemPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + id));
        return mapToPermissionResponse(permission);
    }

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        SystemPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + id));
        permissionRepository.delete(permission);
        log.info("Deleted permission: {}", permission.getName());
    }

    // === Admin Role Assignment ===

    @Override
    @Transactional
    public void assignRoleToAdmin(UUID adminId, String roleName) {
        SystemAdmin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + adminId));
        SystemRole role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        admin.addRole(role);
        adminRepository.save(admin);
        log.info("Assigned role {} to admin {}", roleName, admin.getEmail());
    }

    @Override
    @Transactional
    public void removeRoleFromAdmin(UUID adminId, String roleName) {
        SystemAdmin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + adminId));
        SystemRole role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        admin.removeRole(role);
        adminRepository.save(admin);
        log.info("Removed role {} from admin {}", roleName, admin.getEmail());
    }

    // === Helper Methods ===

    private Set<SystemPermission> resolvePermissions(Set<String> permissionNames) {
        Set<SystemPermission> permissions = new HashSet<>();
        for (String name : permissionNames) {
            SystemPermission permission = permissionRepository.findByName(name)
                    .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + name));
            permissions.add(permission);
        }
        return permissions;
    }

    private RoleResponseDto mapToRoleResponse(SystemRole role) {
        return new RoleResponseDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream()
                        .map(SystemPermission::getName)
                        .collect(Collectors.toSet()),
                role.getCreatedAt()
        );
    }

    private PermissionResponseDto mapToPermissionResponse(SystemPermission permission) {
        return new PermissionResponseDto(
                permission.getId(),
                permission.getName(),
                permission.getDescription(),
                permission.getCreatedAt()
        );
    }
}
