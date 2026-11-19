package com.kizuna.features.ticket.unit;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.ticket.dto.SupportTicketRequests.Create;
import com.kizuna.features.ticket.service.TicketAttachmentValidator.ValidatedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Gửi yêu cầu hỗ trợ - SupportTicketCommandExecutor.create(Create, files).
 * Mỗi test là một cột UTCID trong bảng test case. Căn cứ: spec 010 FR-001, FR-002, FR-014, FR-020; quy tắc ưu tiên ban đầu.
 */
class TicketCreateTest extends TicketUnitTestBase {
    private static final UUID BOOKING = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(contexts.bookingStart(any())).thenReturn(Optional.empty());
        when(tickets.find(any(), eq(false))).thenReturn(new HashMap<>(Map.of("status", "OPEN")));
        when(messages.append(any(), any(), any())).thenReturn(UUID.randomUUID());
    }

    private static Create req(String subject, String category, String message) { return new Create(subject, category, message, null, null, null); }

    private void nothingCreated() {
        verify(tickets, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
        verify(messages, never()).append(any(), any(), any());
        verifyNoInteractions(notifier);
    }

    // ---------- Thành công (N) ----------

    @Test @DisplayName("UTCID01: Học viên, nhóm Tài khoản, dữ liệu hợp lệ -> tạo yêu cầu ưu tiên Bình thường, lưu nội dung, ghi lịch sử, báo CS")
    void utcid01_learnerCreatesAccountTicket() {
        as(LEARNER_ACTOR);
        UUID id = executor.create(req("Không đăng nhập được", "ACCOUNT", "Tôi nhập đúng mật khẩu nhưng vẫn báo sai."), List.of());

        verify(tickets).create(eq(id), eq(OWNER), eq("Không đăng nhập được"), eq("ACCOUNT"), eq("MEDIUM"), isNull(), isNull(), isNull());
        verify(messages).append(id, OWNER, "Tôi nhập đúng mật khẩu nhưng vẫn báo sai.");
        verify(audit).append(eq(id), eq(OWNER), eq("CREATE"), eq(0L), any(), any(), isNull());
        verify(notifier).created(any());
    }

    @Test @DisplayName("UTCID02: Gia sư, nhóm Thanh toán -> ưu tiên Cao")
    void utcid02_tutorBillingIsHigh() {
        as(TUTOR_ACTOR);
        executor.create(req("Bị trừ tiền hai lần", "BILLING", "Giao dịch mua gói bị trừ hai lần."), List.of());
        verify(tickets).create(any(), eq(OWNER), any(), eq("BILLING"), eq("HIGH"), any(), any(), any());
    }

    @Test @DisplayName("UTCID03: Gắn buổi học thử bắt đầu trong 24 giờ tới, kèm 1 tệp -> ưu tiên Khẩn cấp, lưu tệp đính kèm")
    void utcid03_bookingSoonIsUrgentWithAttachment() {
        as(LEARNER_ACTOR);
        when(contexts.bookingStart(BOOKING)).thenReturn(Optional.of(OffsetDateTime.now().plusHours(3)));
        var file = new ValidatedFile("anh.png", "image/png", new byte[]{1, 2, 3});

        executor.create(new Create("Gia sư chưa gửi link", "BOOKING", "Còn 3 tiếng nữa là học.", BOOKING, null, null), List.of(file));

        verify(tickets).create(any(), any(), any(), eq("BOOKING"), eq("URGENT"), eq(BOOKING), isNull(), isNull());
        verify(attachments).save(any(), any(), eq(OWNER), eq("anh.png"), eq("image/png"), any());
    }

    // ---------- Biên (B) ----------

    @Test @DisplayName("UTCID04: Tiêu đề đúng 200 ký tự (biên trên) -> tạo thành công")
    void utcid04_subject200() {
        as(LEARNER_ACTOR);
        executor.create(req(chars(200), "OTHER", "Nội dung"), List.of());
        verify(tickets).create(any(), any(), eq(chars(200)), eq("OTHER"), eq("LOW"), any(), any(), any());
    }

    @Test @DisplayName("UTCID05: Tiêu đề 201 ký tự (vượt biên) -> 400 'Vui lòng nhập nội dung hợp lệ và đúng độ dài cho phép.'")
    void utcid05_subject201() {
        as(LEARNER_ACTOR);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.create(req(chars(201), "ACCOUNT", "Nội dung"), List.of())).getMessage());
        nothingCreated();
    }

    @Test @DisplayName("UTCID06: Nội dung đúng 10.000 ký tự (biên trên) -> tạo thành công")
    void utcid06_message10000() {
        as(LEARNER_ACTOR);
        executor.create(req("Tiêu đề", "ACCOUNT", chars(10_000)), List.of());
        verify(messages).append(any(), eq(OWNER), eq(chars(10_000)));
    }

    @Test @DisplayName("UTCID07: Nội dung 10.001 ký tự (vượt biên) -> 400")
    void utcid07_message10001() {
        as(LEARNER_ACTOR);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.create(req("Tiêu đề", "ACCOUNT", chars(10_001)), List.of())).getMessage());
        nothingCreated();
    }

    // ---------- Bất thường (A) ----------

    @Test @DisplayName("UTCID08: Tiêu đề chỉ có khoảng trắng -> 400")
    void utcid08_blankSubject() {
        as(LEARNER_ACTOR);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.create(req("   ", "ACCOUNT", "Nội dung"), List.of())).getMessage());
        nothingCreated();
    }

    @Test @DisplayName("UTCID09: Nhóm vấn đề không có trong danh mục -> 400 'Lựa chọn chưa hợp lệ. Vui lòng kiểm tra lại.'")
    void utcid09_unknownCategory() {
        as(LEARNER_ACTOR);
        assertEquals("Lựa chọn chưa hợp lệ. Vui lòng kiểm tra lại.",
                fails(400, () -> executor.create(req("Tiêu đề", "SHOPPING", "Nội dung"), List.of())).getMessage());
        nothingCreated();
    }

    @Test @DisplayName("UTCID10: Nhân viên hỗ trợ tự gửi yêu cầu -> 403 'Chỉ học viên và gia sư được gửi yêu cầu hỗ trợ.'")
    void utcid10_staffCannotCreate() {
        as(CS_ACTOR);
        assertEquals("Chỉ học viên và gia sư được gửi yêu cầu hỗ trợ.",
                fails(403, () -> executor.create(req("Tiêu đề", "ACCOUNT", "Nội dung"), List.of())).getMessage());
        nothingCreated();
    }

    @Test @DisplayName("UTCID11: Gắn buổi học không thuộc tài khoản mình -> 400 'Thông tin liên quan không tồn tại hoặc không thuộc tài khoản của bạn.'")
    void utcid11_relatedBookingOfSomeoneElse() {
        as(LEARNER_ACTOR);
        var r = new Create("Tiêu đề", "BOOKING", "Nội dung", BOOKING, null, null);
        doThrow(new BusinessException(400, "Thông tin liên quan không tồn tại hoặc không thuộc tài khoản của bạn.")).when(contexts).validate(OWNER, r);
        assertEquals("Thông tin liên quan không tồn tại hoặc không thuộc tài khoản của bạn.", fails(400, () -> executor.create(r, List.of())).getMessage());
        nothingCreated();
    }

    @Test @DisplayName("UTCID12: Chưa đăng nhập -> 401 'Vui lòng đăng nhập để tiếp tục.'")
    void utcid12_notLoggedIn() {
        notLoggedIn();
        fails(401, () -> executor.create(req("Tiêu đề", "ACCOUNT", "Nội dung"), List.of()));
        nothingCreated();
    }
}
