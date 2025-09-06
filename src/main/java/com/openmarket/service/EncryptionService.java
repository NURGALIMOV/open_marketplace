package com.openmarket.service;

import com.openmarket.dto.auth.EncryptionResult;
import com.openmarket.exception.AppSystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for AES-GCM encryption/decryption of sensitive data
 * Used for encrypting API keys and other sensitive information
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final String DEFAULT_MASTER_KEY = "default-master-key-change-in-production";

    private final SecretKey masterKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${app.security.encryption.master-key}") String masterKeyString) {
        this.masterKey = createKeyFromString(masterKeyString);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypt plaintext using AES-GCM
     * @param plaintext The text to encrypt
     * @return EncryptionResult containing encrypted data and IV
     */
    public EncryptionResult encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptionResult(encryptedData, iv);
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new AppSystemException("Encryption failed", e);
        }
    }

    /**
     * Decrypt encrypted data using AES-GCM
     * @param encryptedData The encrypted data
     * @param iv The initialization vector used during encryption
     * @return The decrypted plaintext
     */
    public String decrypt(byte[] encryptedData, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new AppSystemException("Decryption failed", e);
        }
    }

    /**
     * Create a SecretKey from a string (for development purposes)
     * In production, use a proper key management system
     */
    private SecretKey createKeyFromString(String keyString) {
        try {
            return DEFAULT_MASTER_KEY.equals(keyString) ? getSecretKeyForTesting() : getSecretKeySpec(keyString);
        } catch (Exception e) {
            log.error("Error creating encryption key", e);
            throw new AppSystemException("Failed to create encryption key", e);
        }
    }

    /**
     * Get SecretKeySpec from a string. Try to decode as base64 first, if not base64, generate key from string hash.
     *
     * @param keyString - key as string
     * @return SecretKeySpec
     */
    private static SecretKeySpec getSecretKeySpec(String keyString) throws NoSuchAlgorithmException {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (IllegalArgumentException e) {
            log.warn("Encryption key is not base64 encoded, generating from string hash. Use base64 in production!");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyString.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        }
    }

    /**
     * For development - in production use proper key derivation
     *
     * @return secret key
     */
    private static SecretKey getSecretKeyForTesting() throws NoSuchAlgorithmException {
        log.warn("Using default encryption key! Change this in production!");
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(256);
        return keyGen.generateKey();
    }
}
