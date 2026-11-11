package com.kizuna.features.booking.entity;

/**
 * Loại yêu cầu kết nối (cột connection_requests.request_type).
 * TRIAL: sinh ra khi học viên đăng ký học thử; DIRECT: học viên kết nối thẳng, không cần học thử.
 */
public enum ConnectionRequestType {
    TRIAL,
    DIRECT
}
