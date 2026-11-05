package com.kizuna.features.payment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Dữ liệu gửi lên của API FE-09. Mã định danh nhận dạng chuỗi để lỗi định dạng trả 400 thân thiện thay vì lỗi hệ thống.
 */
public final class MembershipRequests {

    private MembershipRequests() {
    }

    public record CreatePaymentRequest(
            @NotBlank(message = "Vui lòng chọn gói hội viên.") String planId) {
    }

    public record CreatePlanRequest(
            @NotBlank(message = "Vui lòng nhập tên gói.")
            @Size(max = 100, message = "Tên gói tối đa 100 ký tự.") String planName,
            @NotNull(message = "Vui lòng nhập số tháng.")
            @Min(value = 1, message = "Số tháng phải từ 1 đến 36.")
            @Max(value = 36, message = "Số tháng phải từ 1 đến 36.") Integer durationMonths,
            @NotNull(message = "Vui lòng nhập giá gói.") Long price,
            List<String> benefits) {
    }

    public record UpdatePlanRequest(
            @NotBlank(message = "Vui lòng nhập tên gói.")
            @Size(max = 100, message = "Tên gói tối đa 100 ký tự.") String planName,
            @NotNull(message = "Vui lòng nhập giá gói.") Long price,
            List<String> benefits) {
    }

    public record NoteRequest(
            @NotBlank(message = "Vui lòng nhập nội dung ghi chú.")
            @Size(max = 2000, message = "Ghi chú tối đa 2.000 ký tự.") String note) {
    }
}
