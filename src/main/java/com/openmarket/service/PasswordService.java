package com.openmarket.service;

import com.openmarket.exception.AppSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Service for secure password hashing using BCrypt
 */
@Service
@Slf4j
public class PasswordService {

    /** Strong cost factor */
    private static final int BCRYPT_ROUNDS = 12;
    private final BCryptPasswordEncoder encoder;

    public PasswordService() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_ROUNDS);
    }

    /**
     * Hash a password using BCryptю
     *
     * @param password The plaintext password
     * @return The hashed password
     */
    public String hashPassword(String password) {
        try {
            return encoder.encode(password);
        } catch (Exception e) {
            log.error("Error hashing password", e);
            throw new AppSystemException("Password hashing failed", e);
        }
    }

    /**
     * Verify a password against its hash.
     *
     * @param password The plaintext password
     * @param hash The stored hash
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String password, String hash) {
        try {
            return encoder.matches(password, hash);
        } catch (Exception e) {
            log.error("Error verifying password", e);
            return false;
        }
    }

    /**
     * Generate password validation error message.
     *
     * @param password The password to validate
     * @return Error message or null if valid
     */
    public Optional<String> getPasswordValidationError(String password) {
        if (Objects.isNull(password) || password.length() < 8) {
            return Optional.of("Password must be at least 8 characters long");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            return Optional.of("Password must contain at least one digit");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            return Optional.of("Password must contain at least one lowercase letter");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return Optional.of("Password must contain at least one uppercase letter");
        }
        return Optional.empty();
    }
}
