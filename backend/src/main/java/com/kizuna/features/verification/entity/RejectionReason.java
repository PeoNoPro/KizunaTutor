package com.kizuna.features.verification.entity;

/**
 * Lý do từ chối hồ sơ xác minh (UC-28 bước 4).
 */
public enum RejectionReason {
    INSUFFICIENT_QUALIFICATION,
    INVALID_OR_FORGED_DOCUMENT,
    INFORMATION_MISMATCH,
    OTHER
}
