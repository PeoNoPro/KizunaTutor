package com.kizuna.features.verification.entity;

/**
 * Loại giấy tờ xác minh gia sư (Assumption A-4 của FE-03).
 * Hồ sơ chỉ được nộp khi có ít nhất một giấy tờ định danh và một giấy tờ năng lực tiếng Nhật.
 */
public enum CredentialType {
    IDENTITY_DOCUMENT,
    JLPT_CERTIFICATE,
    OTHER_JAPANESE_CERTIFICATE,
    DEGREE,
    TEACHING_CERTIFICATE,
    OTHER;

    public boolean isIdentity() {
        return this == IDENTITY_DOCUMENT;
    }

    public boolean isJapaneseProficiency() {
        return this == JLPT_CERTIFICATE || this == OTHER_JAPANESE_CERTIFICATE;
    }
}
