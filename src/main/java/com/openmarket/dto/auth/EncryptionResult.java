package com.openmarket.dto.auth;

import java.util.Arrays;
import java.util.Objects;

/**
 * Result of encryption operation
 */
public record EncryptionResult(byte[] encryptedData, byte[] iv) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptionResult that = (EncryptionResult) o;
        return Objects.deepEquals(iv, that.iv) && Objects.deepEquals(encryptedData, that.encryptedData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(encryptedData), Arrays.hashCode(iv));
    }

    @Override
    public String toString() {
        return "EncryptionResult{encryptedData=%s, iv=%s}".formatted(
                Arrays.toString(encryptedData),
                Arrays.toString(iv)
        );
    }
}
