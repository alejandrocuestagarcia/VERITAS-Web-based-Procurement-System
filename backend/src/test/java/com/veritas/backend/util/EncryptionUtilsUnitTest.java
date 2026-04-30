package com.veritas.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
        
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(original);
        
        String decrypted = encryptionUtils.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void EncryptAndDecrypt_NullInput_ReturnsNull() {
        assertThat(encryptionUtils.encrypt(null)).isNull();
        assertThat(encryptionUtils.decrypt(null)).isNull();
    }

    @Test
    void Decrypt_InvalidCiphertext_ReturnsOriginalText() {
        String notEncrypted = "not-encrypted-text";
        String decrypted = encryptionUtils.decrypt(notEncrypted);
        assertThat(decrypted).isEqualTo(notEncrypted);
    }
}
