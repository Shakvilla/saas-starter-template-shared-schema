package cloud.norgha.multi_tenant_saas_starter_template.modules.users.services.impl;

import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.CreateUserRequestDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.dto.UserResponseDto;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.entity.User;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.repository.UserRepository;
import cloud.norgha.multi_tenant_saas_starter_template.modules.users.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of UserService for managing tenant users.
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {
        log.debug("Creating user with email: {}", request.email());

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),

                request.companyName(),

                "USER" // Default role for new users
        );

        User saved = userRepository.save(user);
        log.info("Created user with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public UserResponseDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        String userId = authentication.getName();
        UUID userUuid = UUID.fromString(userId);

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        return mapToResponse(user);
    }

    @Override
    public List<UserResponseDto> listUsers() {
        log.debug("Listing all users in current tenant");

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID id, String fullName, String email) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }

        User updated = userRepository.save(user);
        log.info("Updated user with ID: {}", id);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user with ID: {}", id);
    }

    private UserResponseDto mapToResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCompanyName(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
