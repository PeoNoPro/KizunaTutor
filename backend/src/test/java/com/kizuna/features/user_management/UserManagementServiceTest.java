package com.kizuna.features.user_management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.user_management.dto.UpdateUserStatusRequest;
import com.kizuna.features.user_management.repository.ManagedUserRepository;
import com.kizuna.features.user_management.repository.ManagedUserRepository.Account;
import com.kizuna.features.user_management.repository.ManagedUserRepository.AccountRow;
import com.kizuna.features.user_management.repository.ManagedUserRepository.LearnerRow;
import com.kizuna.features.user_management.repository.ManagedUserRepository.TutorRow;
import com.kizuna.features.user_management.service.UserStatusMailService;
import com.kizuna.features.user_management.service.impl.UserManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FE-14: quy tắc nghiệp vụ của UserManagementServiceImpl (UC-20, UC-21, UC-12). Repository giả lập, không chạm DB. */
class UserManagementServiceTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MANAGER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-13T08:00:00Z");

    private ManagedUserRepository repository;
    private UserStatusMailService mail;
    private UserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ManagedUserRepository.class);
        mail = mock(UserStatusMailService.class);
        service = new UserManagementServiceImpl(repository, mail, new ObjectMapper());
        when(repository.statusHistory(any(), anyInt())).thenReturn(List.of());
        when(repository.tutorProfile(any())).thenReturn(Optional.empty());
        when(repository.learnerProfile(any())).thenReturn(Optional.empty());
    }

    private static Account account(String role, String status) {
        return new Account(USER, "Nguyễn Văn A", "a@example.com", null, null, role, status, NOW, NOW);
    }

    // ===== Danh sách (UC-20) =====

    @Test
    void danhSach_macDinhGiaSu_chuanHoaTrangVaKichThuoc() {
        when(repository.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ManagedUserRepository.Page(
                List.of(new AccountRow(USER, "A", "a@example.com", "ROLE_TUTOR", "SUSPENDED", NOW)), 21));

        var page = service.list(null, "  ", null, -3, 15);

        verify(repository).list("ROLE_TUTOR", null, null, 0, 20);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.items().get(0).accountStatusLabel()).isEqualTo("Tạm khóa");
        assertThat(page.items().get(0).categoryLabel()).isEqualTo("Gia sư");
    }

    @Test
    void danhSach_hocVien_locTrangThai_tuKhoaThoatKyTuDacBiet() {
        when(repository.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ManagedUserRepository.Page(List.of(), 0));

        var page = service.list("learner", " 50%_a\\b ", "banned", 1, 50);

        verify(repository).list("ROLE_LEARNER", "BANNED", "%50\\%\\_a\\\\b%", 1, 50);
        assertThat(page.items()).isEmpty();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void danhSach_tuKhoaQuaDaiBiCatCon100KyTu() {
        when(repository.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ManagedUserRepository.Page(List.of(), 0));
        service.list("TUTOR", "x".repeat(150), null, 0, 20);
        verify(repository).list("ROLE_TUTOR", null, "%" + "x".repeat(100) + "%", 0, 20);
    }

    @Test
    void danhSach_nhomHoacTrangThaiSai_bao400() {
        assertThatThrownBy(() -> service.list("STAFF", null, null, 0, 20))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(400));
        assertThatThrownBy(() -> service.list("TUTOR", null, "LOCKED", 0, 20))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(400));
    }

    // ===== Chi tiết (UC-21) =====

    @Test
    void chiTiet_taiKhoanNhanVien_hoacKhongTonTai_bao404() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_CS_STAFF", "ACTIVE")));
        assertThatThrownBy(() -> service.detail(USER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(404));

        when(repository.findAccount(USER)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(USER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(404));
    }

    @Test
    void chiTiet_giaSu_coHoSo_soDienThoaiLayTuHoSoKhiTaiKhoanTrong_vaTrangThaiDuocChon() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_TUTOR", "ACTIVE")));
        when(repository.tutorProfile(USER)).thenReturn(Optional.of(new TutorRow(10001L, "Tiêu đề", 3, "N2", null, 0,
                "ACTIVE_PAID", "0901234567", null, "Hà Nội", NOW)));

        var d = service.detail(USER);

        assertThat(d.phone()).isEqualTo("0901234567");
        assertThat(d.tutorProfile().tutorStateLabel()).isEqualTo("Đang Hoạt Động");
        assertThat(d.learnerProfile()).isNull();
        assertThat(d.allowedStatuses()).extracting("value").containsExactly("SUSPENDED", "BANNED");
        assertThat(d.statusChangeBlockedReason()).isNull();
    }

    @Test
    void chiTiet_hocVienChoKichHoat_khongDuocDoiTrangThai() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_LEARNER", "PENDING")));
        when(repository.learnerProfile(USER)).thenReturn(Optional.of(new LearnerRow("N5", "N3", "Giao tiếp", "Hà Nội", null)));

        var d = service.detail(USER);

        assertThat(d.learnerProfile().targetLevel()).isEqualTo("N3");
        assertThat(d.allowedStatuses()).isEmpty();
        assertThat(d.statusChangeBlockedReason()).contains("chưa hoàn tất đăng ký");
        assertThat(d.accountStatusLabel()).isEqualTo("Chờ kích hoạt");
    }

    // ===== Đổi trạng thái (UC-12) =====

    @Test
    void doiTrangThai_hopLe_capNhatGhiNhatKyVaGuiEmail_khongLoLyDoNoiBo() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_TUTOR", "ACTIVE")));
        when(repository.updateStatus(USER, "ACTIVE", "SUSPENDED")).thenReturn(1);

        service.updateStatus(USER, new UpdateUserStatusRequest("suspended", "ACTIVE", "  Vi phạm chính sách  ", "Liên hệ hỗ trợ"), MANAGER);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(repository).insertAudit(eq(MANAGER), eq(USER), json.capture());
        assertThat(json.getValue()).contains("\"fromStatus\":\"ACTIVE\"", "\"toStatus\":\"SUSPENDED\"",
                "\"reason\":\"Vi phạm chính sách\"", "\"messageToUser\":\"Liên hệ hỗ trợ\"");
        verify(mail).statusChangedAfterCommit("a@example.com", "Nguyễn Văn A", "SUSPENDED", "Liên hệ hỗ trợ");
    }

    @Test
    void doiTrangThai_loiNhanTrong_ghiNull() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_LEARNER", "SUSPENDED")));
        when(repository.updateStatus(USER, "SUSPENDED", "ACTIVE")).thenReturn(1);

        service.updateStatus(USER, new UpdateUserStatusRequest("ACTIVE", "SUSPENDED", "Đã xác minh", "   "), MANAGER);

        verify(mail).statusChangedAfterCommit(anyString(), anyString(), eq("ACTIVE"), isNull());
    }

    @Test
    void doiTrangThai_taiKhoanChoKichHoat_bao400_khongGhiGi() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_LEARNER", "PENDING")));

        assertThatThrownBy(() -> service.updateStatus(USER, new UpdateUserStatusRequest("ACTIVE", "PENDING", "x", null), MANAGER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(400));
        verifyNoWrite();
    }

    @Test
    void doiTrangThai_trungTrangThaiHienTai_bao400() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_TUTOR", "BANNED")));

        assertThatThrownBy(() -> service.updateStatus(USER, new UpdateUserStatusRequest("BANNED", "BANNED", "x", null), MANAGER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getMessage()).isEqualTo("Tài khoản đã ở trạng thái này."));
        verifyNoWrite();
    }

    @Test
    void doiTrangThai_trangThaiDichSai_bao400() {
        assertThatThrownBy(() -> service.updateStatus(USER, new UpdateUserStatusRequest("PENDING", "ACTIVE", "x", null), MANAGER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(400));
        verifyNoWrite();
    }

    @Test
    void doiTrangThai_nguoiKhacVuaDoi_bao409() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_TUTOR", "SUSPENDED")));

        assertThatThrownBy(() -> service.updateStatus(USER, new UpdateUserStatusRequest("BANNED", "ACTIVE", "x", null), MANAGER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(409));
        verifyNoWrite();
    }

    @Test
    void doiTrangThai_updateKhongDoiDong_bao409_khongGhiNhatKy() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_TUTOR", "ACTIVE")));
        when(repository.updateStatus(USER, "ACTIVE", "BANNED")).thenReturn(0);

        assertThatThrownBy(() -> service.updateStatus(USER, new UpdateUserStatusRequest("BANNED", "ACTIVE", "x", null), MANAGER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(409));
        verify(repository, never()).insertAudit(any(), any(), any());
        verify(mail, never()).statusChangedAfterCommit(any(), any(), any(), any());
    }

    @Test
    void doiTrangThai_taiKhoanNhanVien_bao404() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_MANAGER", "ACTIVE")));

        assertThatThrownBy(() -> service.updateStatus(USER, new UpdateUserStatusRequest("SUSPENDED", "ACTIVE", "x", null), MANAGER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(404));
        verifyNoWrite();
    }

    private void verifyNoWrite() {
        verify(repository, never()).updateStatus(any(), any(), any());
        verify(repository, never()).insertAudit(any(), any(), any());
        verify(mail, never()).statusChangedAfterCommit(any(), any(), any(), any());
    }
}
