package com.veritas.backend.config;

import com.veritas.backend.util.EncryptionUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Converter
public class JpaEncryptionConverter implements AttributeConverter<String, String> {

    private final EncryptionUtils encryptionUtils;

    // Use @Lazy to avoid circular dependencies
    public JpaEncryptionConverter(@Lazy EncryptionUtils encryptionUtils) {
        this.encryptionUtils = encryptionUtils;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionUtils.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionUtils.decrypt(dbData);
    }
}
