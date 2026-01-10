package cloud.norgha.multi_tenant_saas_starter_template.modules.users.services.impl;

import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.CreateUserRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.UserResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity.User;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.repository.UserRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.services.UserService;
import cloud.norgha.multi_tenant_saas_starter_template.multitenancy.tenant.TenantContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;


@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    protected Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());


    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public UserResponseDto createUser(CreateUserRequestDto request){
        String tenantId = TenantContext.getTenantId();


        User user = new User(
                tenantId,
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );
        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }


    @Override
    public UserResponseDto getCurrentUser() {
        // example placeholder
        throw new UnsupportedOperationException("Implement current user resolution");
    }

    @Override
    public List<UserResponseDto> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private UserResponseDto mapToResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
