package com.kizuna.features.verification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cán bộ học vụ không chấp nhận yêu cầu cập nhật hồ sơ. Lý do 1–2.000 ký tự, kiểm tra ở service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectProfileUpdateRequest {
    private String feedback;
}
