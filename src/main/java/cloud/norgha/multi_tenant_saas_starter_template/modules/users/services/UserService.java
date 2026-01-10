package cloud.norgha.multi_tenant_saas_starter_template.modules.users.services;


import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.CreateUserRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.UserResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity.User;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

public interface UserService {

    UserResponseDto createUser(CreateUserRequestDto request);

    UserResponseDto getCurrentUser();

    List<UserResponseDto> listUsers();

}
