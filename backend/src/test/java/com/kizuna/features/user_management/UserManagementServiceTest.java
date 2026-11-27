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
    private static final OffsetDateTime LAST_LOGIN = OffsetDateTime.parse("2026-11-01T02:30:00Z");

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
        return new Account(USER, "Nguyễn Văn A", "a@example.com", null, null, role, status, NOW, NOW, LAST_LOGIN);
    }

    // ===== Danh sách (UC-20) =====

    @Test
    void danhSach_macDinhGiaSu_chuanHoaTrangVaKichThuoc() {
        when(repository.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ManagedUserRepository.Page(
                List.of(new AccountRow(USER, "A", "a@example.com", "ROLE_TUTOR", "SUSPENDED", NOW, LAST_LOGIN)), 21));

        var page = service.list(null, "  ", null, -3, 15);

        verify(repository).list("ROLE_TUTOR", null, null, 0, 20);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.items().get(0).accountStatusLabel()).isEqualTo("Tạm khóa");
        assertThat(page.items().get(0).categoryLabel()).isEqualTo("Gia sư");
        assertThat(page.items().get(0).lastLoginAt()).isEqualTo(LAST_LOGIN);
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
        assertThat(d.lastLoginAt()).isEqualTo(LAST_LOGIN);
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

        service.updateStatus(USER, new UpdateUserStatusRequest("ACTIVE", "SUSPENDED", "x", "   "), MANAGER);

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

    // ===== REQ-24: danh sách =====

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"10, 10", "20, 20", "50, 50", "0, 20", "9, 20", "11, 20", "49, 20", "51, 20", "100, 20"})
    void req24_onlyPageSizes10_20_50AreUsedOtherwiseDefault20(int requested, int expected) {
        when(repository.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ManagedUserRepository.Page(List.of(), 0));

        var page = service.list("TUTOR", null, null, 0, requested);

        verify(repository).list("ROLE_TUTOR", null, null, 0, expected);
        assertThat(page.size()).isEqualTo(expected);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"PENDING, PENDING", "active, ACTIVE", "Suspended, SUSPENDED", "BANNED, BANNED"})
    void req24_eachAccountStatusCanBeUsedAsFilter(String filter, String expected) {
        when(repository.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ManagedUserRepository.Page(List.of(), 0));

        service.list("LEARNER", null, filter, 0, 20);

        verify(repository).list("ROLE_LEARNER", expected, null, 0, 20);
    }

    @Test
    void req24_percentAndUnderscoreAreSearchedLiterally() {
        when(repository.list(any(), any(), any(), anyInt(), anyInt())).thenReturn(new ManagedUserRepository.Page(List.of(), 0));

        service.list("TUTOR", "a_b%", null, 0, 20);

        verify(repository).list("ROLE_TUTOR", null, "%a\\_b\\%%", 0, 20);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ADMIN", "MANAGER", "CS_STAFF", "ACADEMIC_OFFICER", "ROLE_TUTOR"})
    void req24_staffAndUnknownGroupsAreRejected(String category) {
        assertThatThrownBy(() -> service.list(category, null, null, 0, 20))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.status()).isEqualTo(400);
                    assertThat(e.getMessage()).isEqualTo("Nhóm người dùng không hợp lệ.");
                });
        verify(repository, never()).list(any(), any(), any(), anyInt(), anyInt());
    }

    // ===== REQ-25: chi tiết =====

    @Test
    void req25_historyIsLimitedTo20MostRecentChanges() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_LEARNER", "ACTIVE")));

        service.detail(USER);

        verify(repository).statusHistory(USER, 20);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CS_STAFF", "ROLE_ACADEMIC_OFFICER"})
    void req25_staffAccountsAreNeverReturned(String role) {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account(role, "ACTIVE")));

        assertThatThrownBy(() -> service.detail(USER))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status()).isEqualTo(404));
        verify(repository, never()).statusHistory(any(), anyInt());
    }

    @Test
    void req25_learnerDetailReturnsProfileAndAllowedStatuses() {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_LEARNER", "SUSPENDED")));
        when(repository.learnerProfile(USER)).thenReturn(Optional.of(new LearnerRow("N5", "N3", "Giao tiếp", "Hà Nội", null)));

        var d = service.detail(USER);

        assertThat(d.fullName()).isEqualTo("Nguyễn Văn A");
        assertThat(d.email()).isEqualTo("a@example.com");
        assertThat(d.learnerProfile().currentLevel()).isEqualTo("N5");
        assertThat(d.tutorProfile()).isNull();
        assertThat(d.allowedStatuses()).extracting("value").containsExactly("ACTIVE", "BANNED");
    }

    // ===== REQ-26: đổi trạng thái =====

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   ", "\t"})
    void lyDoNoiBoKhongBatBuoc_trongThiGhiNullVaVanDoiTrangThai(String reason) {
        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_TUTOR", "ACTIVE")));
        when(repository.updateStatus(USER, "ACTIVE", "SUSPENDED")).thenReturn(1);

        service.updateStatus(USER, new UpdateUserStatusRequest("SUSPENDED", "ACTIVE", reason, null), MANAGER);

        verify(repository).updateStatus(USER, "ACTIVE", "SUSPENDED");
        verify(repository).insertAudit(eq(MANAGER), eq(USER), org.mockito.ArgumentMatchers.contains("\"reason\":null"));
        verify(mail).statusChangedAfterCommit(anyString(), anyString(), eq("SUSPENDED"), isNull());
    }

    // ===== REQ-24/25/26: câu thông báo lỗi đúng như requirement =====

    private static void assertBusiness(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, int status, String message) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> {
            assertThat(e.status()).isEqualTo(status);
            assertThat(e.getMessage()).isEqualTo(message);
        });
    }

    @Test
    void req25_notFoundMessageForMissingAndStaffAccounts() {
        when(repository.findAccount(USER)).thenReturn(Optional.empty());
        assertBusiness(() -> service.detail(USER), 404, "Không tìm thấy tài khoản gia sư hoặc học viên này.");

        when(repository.findAccount(USER)).thenReturn(Optional.of(account("ROLE_ADMIN", "ACTIVE")));
        assertBusiness(() -> service.detail(USER), 404, "Không tìm thấy tài khoản gia sư hoặc học viên này.");
    }

    private void verifyNoWrite() {
        verify(repository, never()).updateStatus(any(), any(), any());
        verify(repository, never()).insertAudit(any(), any(), any());
        verify(mail, never()).statusChangedAfterCommit(any(), any(), any(), any());
    }

    /**
     * REQ-26: email báo đổi trạng thái chỉ gửi sau khi giao dịch commit, trên luồng nền;
     * lỗi gửi không lan ra ngoài; nội dung đúng theo trạng thái và lời nhắn được escape.
     */
    @org.junit.jupiter.api.Nested
    class StatusMail {

        private static final long WAIT_MS = 3000;

        private org.springframework.mail.javamail.JavaMailSender sender;
        private UserStatusMailService realMail;

        @BeforeEach
        void setUpMail() {
            sender = mock(org.springframework.mail.javamail.JavaMailSender.class);
            when(sender.createMimeMessage()).thenAnswer(inv ->
                    new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null));
            realMail = new UserStatusMailService(sender, "noreply@kizuna.test");
        }

        @org.junit.jupiter.api.AfterEach
        void clearTransaction() {
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            }
        }

        private jakarta.mail.internet.MimeMessage sentMessage() {
            ArgumentCaptor<jakarta.mail.internet.MimeMessage> sent =
                    ArgumentCaptor.forClass(jakarta.mail.internet.MimeMessage.class);
            verify(sender, org.mockito.Mockito.timeout(WAIT_MS)).send(sent.capture());
            try {
                // Header Content-Type của từng phần chỉ được ghi khi lưu thư, như lúc JavaMailSender gửi thật
                sent.getValue().saveChanges();
            } catch (jakarta.mail.MessagingException e) {
                throw new IllegalStateException(e);
            }
            return sent.getValue();
        }

        private String html(jakarta.mail.Part part) throws Exception {
            Object content = part.getContent();
            if (content instanceof jakarta.mail.Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    String found = html(multipart.getBodyPart(i));
                    if (found != null) {
                        return found;
                    }
                }
                return null;
            }
            return part.isMimeType("text/html") ? (String) content : null;
        }

        @Test
        void req26_missingEmailSendsNothing() {
            realMail.statusChangedAfterCommit(null, "A", "SUSPENDED", null);
            realMail.statusChangedAfterCommit("  ", "A", "SUSPENDED", null);

            verify(sender, org.mockito.Mockito.after(300).never()).createMimeMessage();
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.CsvSource({
                "ACTIVE, Tài khoản KizunaTutor của bạn đã được mở lại",
                "SUSPENDED, Tài khoản KizunaTutor của bạn đã bị tạm khóa",
                "BANNED, Tài khoản KizunaTutor của bạn đã bị khóa vĩnh viễn"
        })
        void req26_subjectAndRecipientFollowNewStatus(String status, String subject) throws Exception {
            realMail.statusChangedAfterCommit("a@example.com", "Nguyễn Văn A", status, null);

            jakarta.mail.internet.MimeMessage message = sentMessage();
            assertThat(message.getSubject()).isEqualTo(subject);
            assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("a@example.com");
            assertThat(((jakarta.mail.internet.InternetAddress) message.getFrom()[0]).getAddress())
                    .isEqualTo("noreply@kizuna.test");
            assertThat(html(message)).contains("Xin chào Nguyễn Văn A,");
        }

        @Test
        void req26_reactivationMailHasNoSupportContactLine() throws Exception {
            realMail.statusChangedAfterCommit("a@example.com", "A", "ACTIVE", null);

            assertThat(html(sentMessage()))
                    .contains("đã được khôi phục quyền truy cập")
                    .doesNotContain("phản hồi email này");
        }

        @Test
        void req26_lockMailHasSupportContactLine() throws Exception {
            realMail.statusChangedAfterCommit("a@example.com", "A", "BANNED", null);

            assertThat(html(sentMessage()))
                    .contains("khóa vĩnh viễn")
                    .contains("phản hồi email này");
        }

        @Test
        void req26_managerMessageIsEscapedAndKeepsLineBreaks() throws Exception {
            realMail.statusChangedAfterCommit("a@example.com", "<b>A</b>", "SUSPENDED",
                    "Line 1\n<script>alert(1)</script>");

            String body = html(sentMessage());
            // Ghi chú: HtmlUtils.htmlEscape còn đổi chữ Latin-1 có dấu (ò, á…) thành entity (&ograve;…); hộp thư vẫn hiển thị đúng.
            assertThat(body)
                    .contains("Lời nhắn từ Quản lý vận hành")
                    .contains("Line 1<br>&lt;script&gt;alert(1)&lt;/script&gt;")
                    .contains("Xin chào &lt;b&gt;A&lt;/b&gt;,")
                    .doesNotContain("<script>");
        }

        @Test
        void req26_noMessageMeansNoMessageBlockAndBlankNameIsGeneric() throws Exception {
            realMail.statusChangedAfterCommit("a@example.com", " ", "SUSPENDED", "   ");

            assertThat(html(sentMessage()))
                    .contains("Xin chào bạn,")
                    .doesNotContain("Lời nhắn từ Quản lý vận hành");
        }

        @Test
        void req26_rolledBackTransactionSendsNoMail() {
            org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();

            realMail.statusChangedAfterCommit("a@example.com", "A", "SUSPENDED", null);
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(sender, org.mockito.Mockito.after(300).never()).send(any(jakarta.mail.internet.MimeMessage.class));
        }

        @Test
        void req26_sendFailureDoesNotPropagateAndLaterMailsStillGo() {
            org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("smtp down"))
                    .doNothing()
                    .when(sender).send(any(jakarta.mail.internet.MimeMessage.class));

            realMail.statusChangedAfterCommit("a@example.com", "A", "SUSPENDED", null);
            realMail.statusChangedAfterCommit("b@example.com", "B", "ACTIVE", null);

            verify(sender, org.mockito.Mockito.timeout(WAIT_MS).times(2)).send(any(jakarta.mail.internet.MimeMessage.class));
        }
    }
}
