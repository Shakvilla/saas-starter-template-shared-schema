package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.impl;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.AdminResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateAdminRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemAdmin;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.SystemRole;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.SystemAdminRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.SystemRoleRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.SystemAdminService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SystemAdminServiceImpl implements SystemAdminService {

    private static final Logger log = LoggerFactory.getLogger(SystemAdminServiceImpl.class);

    private final SystemAdminRepository adminRepository;
    private final SystemRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public SystemAdminServiceImpl(
            SystemAdminRepository adminRepository,
            SystemRoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AdminResponseDto createAdmin(CreateAdminRequestDto request) {
        // Check if email already exists
        if (adminRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Resolve roles
        Set<SystemRole> roles = new HashSet<>();
        if (request.roleNames() != null && !request.roleNames().isEmpty()) {
            for (String roleName : request.roleNames()) {
                SystemRole role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
                roles.add(role);
            }
        }

        // Create admin
        SystemAdmin admin = new SystemAdmin(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );
        admin.setRoles(roles);

        SystemAdmin saved = adminRepository.save(admin);
        log.info("Created system admin: {} with roles: {}", saved.getEmail(), saved.getRoleNames());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminResponseDto> listAdmins() {
        return adminRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponseDto getAdmin(UUID id) {
        SystemAdmin admin = adminRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + id));
        return mapToResponse(admin);
    }

    @Override
    @Transactional
    public void deactivateAdmin(UUID id) {
        SystemAdmin admin = adminRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + id));
        admin.setActive(false);
        adminRepository.save(admin);
        log.info("Deactivated system admin: {}", admin.getEmail());
    }

    private AdminResponseDto mapToResponse(SystemAdmin admin) {
        return new AdminResponseDto(
                admin.getId(),
                admin.getEmail(),
                admin.getFullName(),
                admin.isActive(),
                admin.getCreatedAt()
        );
    }
}
