package com.veritas.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

//AI-GENERATED

class EncryptionUtilsUnitTest {

    private EncryptionUtils encryptionUtils;

    @BeforeEach
    void setUp() {
        encryptionUtils = new EncryptionUtils();
        ReflectionTestUtils.setField(encryptionUtils, "encryptionKey", "test-key-32-chars-long-1234567890");
        ReflectionTestUtils.setField(encryptionUtils, "salt", "deadbeefcafebabe0123456789abcdef");
        encryptionUtils.init();
    }

    @Test
    void EncryptAndDecrypt_ValidInput_ReturnsOriginalText() {
        String original = "my-secret-token";
        String encrypted = encryptionUtils.encrypt(original);
        
        assertAll(
            () -> assertNotNull(encrypted),
            () -> assertNotEquals(original, encrypted)
        );
        
        String decrypted = encryptionUtils.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void EncryptAndDecrypt_NullInput_ReturnsNull() {
        assertAll(
            () -> assertNull(encryptionUtils.encrypt(null)),
            () -> assertNull(encryptionUtils.decrypt(null))
        );
    }

    @Test
    void Decrypt_InvalidCiphertext_ThrowsIllegalStateException() {
        String notEncrypted = "not-encrypted-text";
        assertThrows(IllegalStateException.class, () -> {
            encryptionUtils.decrypt(notEncrypted);
        });
    }
}
