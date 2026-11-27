package com.kizuna.features.admin.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public final class StaffImportRequests {
    private StaffImportRequests() { }
    /** Xác nhận kiểm tra lại từng dòng nên lỗi dòng trả theo dòng, không dùng @Valid cho danh sách. */
    public record Row(int rowNumber,
        @NotBlank(message="Vui lòng nhập họ tên.") @Size(max=100,message="Họ tên tối đa 100 ký tự.") String fullName,
        @NotBlank(message="Vui lòng nhập email.") @Email(message="Email chưa hợp lệ.") @Size(max=254,message="Email quá dài.") String email,
        @Size(max=20,message="Số điện thoại tối đa 20 ký tự.") String phone,
        @NotBlank(message="Vui lòng nhập vai trò.") String role) { }
    public record Confirm(
        @NotEmpty(message="Chưa có dòng nào để tạo.") @Size(max=100,message="Tối đa 100 dòng mỗi lần nhập.") List<Row> rows) { }
}
