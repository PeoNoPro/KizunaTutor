package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.OperationsActor;
import com.kizuna.features.admin.dto.StaffAccountRequests.Create;
import com.kizuna.features.admin.repository.StaffAccountRepository;
import com.kizuna.features.admin.repository.StaffAccountRepository.Account;
import com.kizuna.features.admin.repository.StaffCredentialRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-11: Tạo tài khoản nhân viên - StaffAccountService.create(Create).
 * Mỗi test là một cột UTCID trong bảng test case (Condition / Input / Confirm).
 * Repository, actor resolver, khóa và gửi email đều là mock nên không chạm DB thật.
 */
class StaffAccountServiceCreateTest {
    private static final UUID ADMIN = UUID.randomUUID();
    private static final int ROLE_CS = 4, ROLE_ACADEMIC = 3, ROLE_MANAGER = 2, ROLE_ADMIN = 1, ROLE_INACTIVE = 9;
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-13T03:00:00Z");

    private final StaffAccountRepository staff = mock(StaffAccountRepository.class);
    private final OperationsActorResolver actors = mock(OperationsActorResolver.class);
    private final OperationsCommandExecutor locks = mock(OperationsCommandExecutor.class);
    private final StaffCredentialService credentials = mock(StaffCredentialService.class);
    private final StaffCredentialRepository emails = mock(StaffCredentialRepository.class);
    private final StaffAccountService service = new StaffAccountService(staff, actors, locks, credentials, emails,
            Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        when(actors.requireAdmin()).thenReturn(new OperationsActor(ADMIN, "ROLE_ADMIN", "Admin", "admin@kizuna.vn", false));
        when(staff.emailExists(anyString(), any())).thenReturn(false);
        when(staff.activeRole(ROLE_CS)).thenReturn("ROLE_CS_STAFF");
        when(staff.activeRole(ROLE_ACADEMIC)).thenReturn("ROLE_ACADEMIC_OFFICER");
        when(staff.activeRole(ROLE_MANAGER)).thenReturn("ROLE_MANAGER");
        when(staff.activeRole(ROLE_ADMIN)).thenReturn("ROLE_ADMIN");
        when(staff.activeRole(ROLE_INACTIVE)).thenThrow(new BusinessException(400, "Vui lòng chọn vai trò nội bộ đang hoạt động."));
        // Sau khi tạo, service đọc lại tài khoản để trả về.
        when(staff.get(any(), eq(false))).thenAnswer(inv -> new Account(inv.getArgument(0), "Nguyễn Minh Anh", "nhanvien.test01@example.com",
                "0901000001", "ACTIVE", NOW, ROLE_CS, "ROLE_CS_STAFF", true, true, "PENDING", null));
    }

    private static Create req(String name, String email, String phone, Integer role, String reason) {
        return new Create(name, email, phone, role, reason);
    }

    private BusinessException fails(Create r) {
        return assertThrows(BusinessException.class, () -> service.create(r));
    }

    private void nothingSaved() {
        verify(staff, never()).create(any(), any(), any(), any(), anyInt(), any(), any());
        verify(credentials, never()).enqueue(any(), any(), any());
    }

    // ---------- Thành công (N) ----------

    @Test @DisplayName("UTCID01: CS Staff, dữ liệu hợp lệ, có SĐT -> tạo tài khoản, ghi nhật ký, xếp email mật khẩu tạm")
    void utcid01_createCsStaff() {
        var result = service.create(req("Nguyễn Minh Anh", "nhanvien.test01@example.com", "0901000001", ROLE_CS, null));

        verify(staff).create(any(), eq("Nguyễn Minh Anh"), eq("nhanvien.test01@example.com"), eq("0901000001"), eq(ROLE_CS),
                eq(StaffCredentialService.UNUSABLE_HASH), eq(NOW));
        verify(staff).audit(eq(ADMIN), any(), eq("CREATE"), eq("Tạo tài khoản nhân viên"), eq(NOW));
        verify(credentials).enqueue(any(), eq(ADMIN), eq(NOW));
        assertEquals("ACTIVE", result.get("status"));
        assertEquals(true, result.get("mustChangePassword"));
    }

    @Test @DisplayName("UTCID02: Cán bộ học vụ, không nhập SĐT -> tạo thành công, SĐT = null")
    void utcid02_createAcademicWithoutPhone() {
        service.create(req("Đỗ Hải Nam", "nhanvien.test09@example.com", null, ROLE_ACADEMIC, null));

        verify(staff).create(any(), eq("Đỗ Hải Nam"), eq("nhanvien.test09@example.com"), isNull(), eq(ROLE_ACADEMIC), any(), any());
    }

    @Test @DisplayName("UTCID03: Email viết hoa + khoảng trắng, họ tên/SĐT có khoảng trắng -> được chuẩn hóa trước khi lưu")
    void utcid03_normalizeInput() {
        service.create(req("  Trần Thu Hà  ", "  NhanVien.Test02@Example.COM ", " 0901000002 ", ROLE_CS, null));

        verify(staff).emailExists(eq("nhanvien.test02@example.com"), any());
        verify(staff).create(any(), eq("Trần Thu Hà"), eq("nhanvien.test02@example.com"), eq("0901000002"), eq(ROLE_CS), any(), any());
    }

    @Test @DisplayName("UTCID04: Quản lý có căn cứ bổ nhiệm -> tạo thành công, nhật ký ghi căn cứ")
    void utcid04_createManagerWithReason() {
        service.create(req("Lê Quốc Bảo", "nhanvien.test03@example.com", "0901000003", ROLE_MANAGER, "Quyết định bổ nhiệm số 12/QĐ"));

        verify(staff).audit(eq(ADMIN), any(), eq("CREATE"), eq("Tạo tài khoản Quản lý. Căn cứ: Quyết định bổ nhiệm số 12/QĐ"), any());
        verify(credentials).enqueue(any(), eq(ADMIN), any());
    }

    // ---------- Lỗi nghiệp vụ (A) ----------

    @Test @DisplayName("UTCID05: Quản lý nhưng căn cứ bổ nhiệm để trống -> 400")
    void utcid05_managerWithoutReason() {
        var ex = fails(req("Lê Quốc Bảo", "nhanvien.test03@example.com", null, ROLE_MANAGER, "   "));

        assertEquals(400, ex.status());
        assertEquals("Vui lòng nhập lý do hoặc căn cứ bổ nhiệm Quản lý.", ex.getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID06: Email đã tồn tại (tài khoản đang hoạt động) -> 400 email đã được sử dụng")
    void utcid06_emailUsed() {
        when(staff.emailExists(eq("cs@kizuna.vn"), any())).thenReturn(true);
        when(staff.lockedStaffEmails(List.of("cs@kizuna.vn"))).thenReturn(Set.of());

        var ex = fails(req("Phạm Ngọc Lan", "cs@kizuna.vn", null, ROLE_CS, null));

        assertEquals(400, ex.status());
        assertEquals("Email này đã được sử dụng. Vui lòng chọn email khác.", ex.getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID07: Email thuộc nhân viên đang bị khóa -> 400, gợi ý mở lại tài khoản cũ")
    void utcid07_emailOfLockedStaff() {
        when(staff.emailExists(eq("locked@kizuna.vn"), any())).thenReturn(true);
        when(staff.lockedStaffEmails(List.of("locked@kizuna.vn"))).thenReturn(Set.of("locked@kizuna.vn"));

        var ex = fails(req("Phạm Ngọc Lan", "locked@kizuna.vn", null, ROLE_CS, null));

        assertEquals(400, ex.status());
        assertEquals(StaffAccountService.LOCKED_EMAIL_MESSAGE, ex.getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Chọn vai trò Quản trị viên -> 400 không cấp quyền Admin")
    void utcid08_adminRoleRejected() {
        var ex = fails(req("Hoàng Đức Huy", "nhanvien.test05@example.com", null, ROLE_ADMIN, null));

        assertEquals(400, ex.status());
        assertEquals("Không cấp quyền Quản trị viên khi tạo tài khoản.", ex.getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID09: Vai trò ngừng sử dụng / không phải vai trò nội bộ -> 400")
    void utcid09_inactiveRole() {
        var ex = fails(req("Hoàng Đức Huy", "nhanvien.test05@example.com", null, ROLE_INACTIVE, null));

        assertEquals(400, ex.status());
        assertEquals("Vui lòng chọn vai trò nội bộ đang hoạt động.", ex.getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID10: Người gọi không phải Admin -> 403")
    void utcid10_notAdmin() {
        when(actors.requireAdmin()).thenThrow(new BusinessException(403, "Bạn không có quyền sử dụng chức năng này."));

        var ex = fails(req("Vũ Thanh Mai", "nhanvien.test06@example.com", null, ROLE_ACADEMIC, null));

        assertEquals(403, ex.status());
        nothingSaved();
    }

    // ---------- Kiểm tra dữ liệu nhập (DTO, @Valid ở controller) ----------

    private Set<String> errors(Create r) {
        var set = new HashSet<String>();
        validator.validate(r).forEach(v -> set.add(v.getMessage()));
        return set;
    }

    @Test @DisplayName("UTCID11: Họ tên trống -> 'Vui lòng nhập họ tên.'")
    void utcid11_blankName() {
        assertTrue(errors(req(" ", "a@example.com", null, ROLE_CS, null)).contains("Vui lòng nhập họ tên."));
    }

    @Test @DisplayName("UTCID12: Họ tên 100 ký tự hợp lệ (biên), 101 ký tự bị chặn")
    void utcid12_nameBoundary() {
        assertTrue(errors(req("A".repeat(100), "a@example.com", null, ROLE_CS, null)).isEmpty());
        assertTrue(errors(req("A".repeat(101), "a@example.com", null, ROLE_CS, null)).contains("Họ tên tối đa 100 ký tự."));
    }

    @Test @DisplayName("UTCID13: Email sai định dạng -> 'Email chưa hợp lệ.'")
    void utcid13_invalidEmail() {
        assertTrue(errors(req("Ngô Bảo Châu", "nhanvien.test10", null, ROLE_CS, null)).contains("Email chưa hợp lệ."));
    }

    @Test @DisplayName("UTCID14: SĐT 20 ký tự hợp lệ (biên), 21 ký tự bị chặn; chưa chọn vai trò bị chặn")
    void utcid14_phoneBoundaryAndMissingRole() {
        assertTrue(errors(req("Ngô Bảo Châu", "a@example.com", "0".repeat(20), ROLE_CS, null)).isEmpty());
        assertTrue(errors(req("Ngô Bảo Châu", "a@example.com", "0".repeat(21), ROLE_CS, null)).contains("Số điện thoại tối đa 20 ký tự."));
        assertTrue(errors(req("Ngô Bảo Châu", "a@example.com", null, null, null)).contains("Vui lòng chọn vai trò."));
    }
}
