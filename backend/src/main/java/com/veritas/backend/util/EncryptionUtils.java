package com.veritas.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class EncryptionUtils {

    @Value("${security.encryption.key}")
    private String encryptionKey;

    @Value("${security.encryption.salt}")
    private String salt;

    private TextEncryptor encryptor;

    @PostConstruct
    public void init() {
        this.encryptor = Encryptors.text(encryptionKey, salt);
    }

    public String encrypt(String text) {
        if (text == null) {
            return null;
        }
        return encryptor.encrypt(text);
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            return encryptor.decrypt(encryptedText);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unable to decrypt credential. " +
                    "Check encryption configuration.", e);
        }
    }
}

