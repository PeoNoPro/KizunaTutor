package com.kizuna.features.verification.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Cột credential_type là VARCHAR tự do và đã có dữ liệu nhập trước FE-03.
 * Giá trị không thuộc CredentialType được đọc thành OTHER thay vì làm hỏng việc tải hồ sơ.
 */
@Converter
public class CredentialTypeConverter implements AttributeConverter<CredentialType, String> {

    @Override
    public String convertToDatabaseColumn(CredentialType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public CredentialType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return CredentialType.OTHER;
        }
        try {
            return CredentialType.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CredentialType.OTHER;
        }
    }
}
