package cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.impl;

import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.CreateTenantRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.TenantResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.dto.UpdateTenantRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.entity.Tenant;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.repository.TenantRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.admin.services.TenantService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of TenantService for tenant management.
 * Evicts tenant cache when tenants are modified.
 */
@Service
@Transactional(readOnly = true)
public class TenantServiceImpl implements TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantServiceImpl.class);

    private final TenantRepository tenantRepository;

    public TenantServiceImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public TenantResponseDto createTenant(CreateTenantRequestDto request) {
        log.debug("Creating tenant with ID: {}", request.id());

        if (tenantRepository.existsById(request.id())) {
            throw new IllegalArgumentException("Tenant already exists with ID: " + request.id());
        }

        Tenant tenant = new Tenant(request.id(), request.name());
        Tenant saved = tenantRepository.save(tenant);

        log.info("Created tenant: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public List<TenantResponseDto> listTenants() {
        log.debug("Listing all tenants");
        return tenantRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TenantResponseDto getTenant(String id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));
        return mapToResponse(tenant);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tenants", key = "#id")
    public TenantResponseDto updateTenant(String id, UpdateTenantRequestDto request) {
        log.debug("Updating tenant: {}", id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            tenant.setName(request.name());
        }
        if (request.active() != null) {
            tenant.setActive(request.active());
        }

        Tenant updated = tenantRepository.save(tenant);
        log.info("Updated tenant: {} (cache evicted)", id);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tenants", key = "#id")
    public void deactivateTenant(String id) {
        log.debug("Deactivating tenant: {}", id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + id));

        tenant.setActive(false);
        tenantRepository.save(tenant);
        log.info("Deactivated tenant: {} (cache evicted)", id);
    }

    private TenantResponseDto mapToResponse(Tenant tenant) {
        return new TenantResponseDto(
                tenant.getId(),
                tenant.getName(),
                tenant.isActive(),
                tenant.getCreatedAt()
        );
    }
}
