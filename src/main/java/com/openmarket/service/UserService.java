package com.openmarket.service;

import com.openmarket.dto.Role;
import com.openmarket.dto.user.CreateUserRequest;
import com.openmarket.dto.user.UserResponse;
import com.openmarket.entity.User;
import com.openmarket.exception.AppAlreadyExistException;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.AuditLogRepository;
import com.openmarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements ApplicationRunner {

    private static final String EMAIL_KEY = "email";
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordService passwordService;

    @Value("${app.security.admin.initial-email}")
    private String adminEmail;

    @Value("${app.security.admin.initial-password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        initializeAdminUser();
    }

    /**
     * Initialize admin user if no admin exists
     */
    private void initializeAdminUser() {
        long adminCount = userRepository.countAdmins();
        if (adminCount == 0) {
            log.info("No admin users found. Creating initial admin user...");
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordService.hashPassword(adminPassword));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Initial admin user created with email: {}", adminEmail);
            log.warn("Please change the default admin password immediately!");
            Map<String, Object> changes = Map.of(EMAIL_KEY, adminEmail, "source", "initialization");
            auditLogRepository.saveUserAuditLog(admin.getId(), "CREATE_ADMIN", changes);
        }
    }

    /**
     * Create a new user (admin only)
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request, UUID actorId) {
        if (userRepository.existsByEmail(request.getEmail())) {
            String message = "User with email %s  already exists".formatted(request.getEmail());
            throw new AppAlreadyExistException(message);
        }
        passwordService.getPasswordValidationError(request.getPassword()).ifPresent(passwordError -> {
            throw new AppBusinessException(passwordError);
        });
        User newUser = userRepository.save(buildUser(request));
        Map<String, Object> changes = Map.of(EMAIL_KEY, newUser.getEmail(), "role", newUser.getRole().name());
        auditLogRepository.saveUserAuditLog(actorId, "CREATE_USER", changes, newUser);
        log.info("User created: {} with role: {}", newUser.getEmail(), newUser.getRole());
        return UserResponse.from(newUser);
    }

    private User buildUser(CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        String passwordHash = passwordService.hashPassword(request.getPassword());
        user.setPasswordHash(passwordHash);
        user.setRole(request.getRole());
        return user;
    }

    /**
     * Get all users (admin only)
     */
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    /**
     * Get users by role
     */
    public Page<UserResponse> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(UserResponse::from);
    }

    /**
     * Update user password (admin only)
     */
    @Transactional
    public UserResponse updateUserPassword(UUID userId, String newPassword, UUID actorId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppNotFoundException("User not found"));
        passwordService.getPasswordValidationError(newPassword).ifPresent(passwordError -> {
            throw new AppBusinessException(passwordError);
        });
        user.setPasswordHash(passwordService.hashPassword(newPassword));
        user = userRepository.save(user);
        Map<String, Object> changes = Map.of("targetUserEmail", user.getEmail(), "passwordChanged", true);
        auditLogRepository.saveUserAuditLog(actorId, "UPDATE_PASSWORD", changes, user);
        log.info("Password updated for user: {} by admin: {}", user.getEmail(), actorId);
        return UserResponse.from(user);
    }

    /**
     * Authenticate user with email and password
     */
    public Optional<User> authenticate(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            Map<String, Object> reason = Map.of(EMAIL_KEY, email, "reason", "user_not_found");
            auditLogRepository.saveUserAuditLog(null, "LOGIN_FAILED", reason);
            return new AppNotFoundException("User not found");
        });
        return authenticate(password, user);
    }

    private Optional<User> authenticate(String password, User user) {
        if (passwordService.verifyPassword(password, user.getPasswordHash())) {
            auditLogRepository.saveUserAuditLog(user.getId(), "LOGIN_SUCCESS", Map.of());
            return Optional.of(user);
        } else {
            Map<String, Object> reason = Map.of("reason", "invalid_password");
            auditLogRepository.saveUserAuditLog(user.getId(), "LOGIN_FAILED", reason);
            return Optional.empty();
        }
    }
}
