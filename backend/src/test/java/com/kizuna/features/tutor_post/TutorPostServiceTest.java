package com.kizuna.features.tutor_post;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.payment.service.TutorMembershipService;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.MyPostView;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.PriceItemRequest;
import com.kizuna.features.tutor_post.entity.PostPriceItem;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.RejectPostRequest;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.SubmitPostRequest;
import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.entity.TutorPostStatus;
import com.kizuna.features.tutor_post.repository.TutorPostRepository;
import com.kizuna.features.tutor_post.service.TutorPostMailService;
import com.kizuna.features.tutor_post.service.TutorPostService;
import com.kizuna.features.notification.service.NotificationEvents;
import org.springframework.test.util.ReflectionTestUtils;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Bài đăng gia sư: chỉ gửi khi có gói hội viên còn hạn và hồ sơ đã duyệt; gửi lại chỉ khi được yêu cầu bổ sung;
 * cán bộ học vụ duyệt / từ chối / yêu cầu bổ sung bài đang chờ duyệt.
 */
class TutorPostServiceTest {

    TutorPostRepository postRepository = mock(TutorPostRepository.class);
    TutorRepository tutorRepository = mock(TutorRepository.class);
    TutorMembershipService membershipService = mock(TutorMembershipService.class);
    UserRepository userRepository = mock(UserRepository.class);
    TutorPostService service = new TutorPostService(postRepository, tutorRepository, membershipService, userRepository);

    User user = User.builder().userId(UUID.randomUUID()).email("gs@kizuna.test").fullName("Gia sư A").build();
    User officer = User.builder().userId(UUID.randomUUID()).email("hv@kizuna.test").fullName("Cán bộ B").build();
    Tutor tutor = Tutor.builder().tutorId(UUID.randomUUID()).user(user)
            .verificationStatus(TutorVerificationStatus.APPROVED).build();

    static SubmitPostRequest requestWith(List<PriceItemRequest> prices) {
        return new SubmitPostRequest("Luyện thi JLPT N3 cấp tốc", "Giới thiệu ".repeat(10),
                "JLPT N3, Kaiwa", "online", prices);
    }

    static SubmitPostRequest validRequest() {
        return requestWith(List.of(new PriceItemRequest("JLPT_N3", 90, new BigDecimal("250000"))));
    }

    TutorPost post(TutorPostStatus status) {
        return TutorPost.builder().postId(UUID.randomUUID()).tutor(tutor).status(status)
                .title("Luyện thi JLPT N3 cấp tốc").content("Giới thiệu ".repeat(10))
                .specialties("JLPT N3").teachingFormat("ONLINE").hourlyRate(new BigDecimal("250000")).build();
    }

    @BeforeEach
    void setUp() {
        when(tutorRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(tutor));
        when(membershipService.hasActiveMembership(eq(tutor.getTutorId()), any())).thenReturn(true);
        when(membershipService.activeMembershipEndDate(eq(tutor.getTutorId()), any())).thenReturn(Optional.of(LocalDate.now().plusDays(30)));
        when(postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId())).thenReturn(Optional.empty());
        when(postRepository.findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(any(), any())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(officer.getUserId())).thenReturn(officer);
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    class TutorSide {

        @Test
        void tutorWithoutMembershipCannotPost() {
            when(membershipService.hasActiveMembership(eq(tutor.getTutorId()), any())).thenReturn(false);

            MyPostView mine = service.getMine(user);
            assertFalse(mine.canPost());
            assertFalse(mine.editable());
            assertEquals(TutorPostService.MSG_NEED_MEMBERSHIP, mine.blockedReason());

            assertEquals(TutorPostService.MSG_NEED_MEMBERSHIP,
                    assertThrows(ConflictException.class, () -> service.submit(user, validRequest())).getMessage());
            verify(postRepository, never()).saveAndFlush(any());
        }

        @Test
        void unapprovedTutorCannotPost() {
            tutor.setVerificationStatus(TutorVerificationStatus.PENDING);
            assertEquals(TutorPostService.MSG_NEED_APPROVED,
                    assertThrows(ConflictException.class, () -> service.submit(user, validRequest())).getMessage());
        }

        @Test
        void firstSubmitCreatesPendingPostWithNormalizedFields() {
            assertTrue(service.getMine(user).editable());

            service.submit(user, validRequest());

            ArgumentCaptor<TutorPost> saved = ArgumentCaptor.forClass(TutorPost.class);
            verify(postRepository).saveAndFlush(saved.capture());
            assertEquals(TutorPostStatus.PENDING_REVIEW, saved.getValue().getStatus());
            assertEquals("ONLINE", saved.getValue().getTeachingFormat());
            assertSame(tutor, saved.getValue().getTutor());
        }

        @Test
        void invalidContentIsRejectedWithFriendlyMessage() {
            SubmitPostRequest bad = new SubmitPostRequest("Ngắn", "Ngắn", null, "",
                    List.of(new PriceItemRequest("JLPT_N3", 90, new BigDecimal("5000"))));
            BadRequestException e = assertThrows(BadRequestException.class, () -> service.submit(user, bad));
            assertTrue(e.getMessage().contains("Tiêu đề từ 10 đến 150 ký tự"));
            assertTrue(e.getMessage().contains("Học phí mỗi buổi"));
            verify(postRepository, never()).saveAndFlush(any());
        }

        // ----- Bảng học phí theo môn học / cấp độ -----

        @Test
        void savesPriceTableSortedAndUsesLowestAsHourlyRate() {
            service.submit(user, requestWith(List.of(
                    new PriceItemRequest("KAIWA", 60, new BigDecimal("180000")),
                    new PriceItemRequest("jlpt_n2", 120, new BigDecimal("300000")),
                    new PriceItemRequest("JLPT_N5", 90, new BigDecimal("150000")))));

            ArgumentCaptor<TutorPost> saved = ArgumentCaptor.forClass(TutorPost.class);
            verify(postRepository).saveAndFlush(saved.capture());
            List<PostPriceItem> items = saved.getValue().getPriceItems();
            assertEquals(List.of("JLPT_N5", "JLPT_N2", "KAIWA"), items.stream().map(PostPriceItem::level).toList());
            // Lưu đúng thời lượng + giá một buổi gia sư nhập; giá quy ra một giờ được tự tính
            assertEquals(90, items.get(0).sessionMinutes());
            assertEquals(new BigDecimal("150000"), items.get(0).pricePerSession());
            assertEquals(new BigDecimal("100000"), items.get(0).hourlyRate());
            assertEquals(new BigDecimal("150000"), items.get(1).hourlyRate());
            assertEquals(new BigDecimal("180000"), items.get(2).hourlyRate());
            // Cột hourly_rate = giá thấp nhất quy ra một giờ (trang Tìm gia sư lọc / sắp xếp theo cột này)
            assertEquals(new BigDecimal("100000"), saved.getValue().getHourlyRate());

            var views = TutorPostService.priceViews(saved.getValue());
            assertEquals("JLPT N5", views.get(0).levelLabel());
            assertEquals(90, views.get(0).sessionMinutes());
            assertEquals(new BigDecimal("150000"), views.get(0).pricePerSession());
            assertEquals("Giao tiếp (Kaiwa)", views.get(2).levelLabel());
        }

        @Test
        void rejectsMissingOrUnsupportedSessionMinutes() {
            BadRequestException missing = assertThrows(BadRequestException.class, () -> service.submit(user, requestWith(List.of(
                    new PriceItemRequest("JLPT_N3", null, new BigDecimal("200000"))))));
            assertTrue(missing.getMessage().contains("thời lượng mỗi buổi"));

            BadRequestException odd = assertThrows(BadRequestException.class, () -> service.submit(user, requestWith(List.of(
                    new PriceItemRequest("JLPT_N3", 100, new BigDecimal("200000"))))));
            assertTrue(odd.getMessage().contains("60, 90 hoặc 120 phút"));
            verify(postRepository, never()).saveAndFlush(any());
        }

        @Test
        void myPostViewListsSessionMinutesOptions() {
            assertEquals(List.of(60, 90, 120), service.getMine(user).sessionMinutesOptions());
        }

        @Test
        void priceTableIsRequired() {
            BadRequestException e = assertThrows(BadRequestException.class, () -> service.submit(user, requestWith(List.of())));
            assertTrue(e.getMessage().contains("ít nhất một mức học phí"));
        }

        @Test
        void rejectsDuplicateUnknownLevelAndOutOfRangeRate() {
            BadRequestException dup = assertThrows(BadRequestException.class, () -> service.submit(user, requestWith(List.of(
                    new PriceItemRequest("JLPT_N3", 90, new BigDecimal("200000")),
                    new PriceItemRequest("JLPT_N3", 90, new BigDecimal("250000"))))));
            assertTrue(dup.getMessage().contains("chỉ được đặt một mức học phí"));

            BadRequestException unknown = assertThrows(BadRequestException.class, () -> service.submit(user, requestWith(List.of(
                    new PriceItemRequest("TOEIC", 90, new BigDecimal("200000"))))));
            assertTrue(unknown.getMessage().contains("chọn môn học hoặc cấp độ"));

            BadRequestException rate = assertThrows(BadRequestException.class, () -> service.submit(user, requestWith(List.of(
                    new PriceItemRequest("JLPT_N1", 90, new BigDecimal("20000000"))))));
            assertTrue(rate.getMessage().contains("Học phí mỗi buổi từ 10.000 đ đến 10.000.000 đ"));
            verify(postRepository, never()).saveAndFlush(any());
        }

        @Test
        void myPostViewListsAllPriceLevels() {
            MyPostView mine = service.getMine(user);
            assertEquals(8, mine.priceLevels().size());
            assertEquals("JLPT_N5", mine.priceLevels().get(0).code());
            assertEquals("Luyện thi EJU", mine.priceLevels().get(7).label());
        }

        @Test
        void legacyPostWithoutPriceTableHasEmptyPriceViews() {
            assertTrue(TutorPostService.priceViews(post(TutorPostStatus.PUBLISHED)).isEmpty());
        }

        @Test
        void cannotSubmitWhilePendingOrAfterFirstPostRejected() {
            for (TutorPostStatus status : new TutorPostStatus[]{TutorPostStatus.PENDING_REVIEW, TutorPostStatus.REJECTED}) {
                when(postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId()))
                        .thenReturn(Optional.of(post(status)));

                assertFalse(service.getMine(user).editable(), "không được sửa khi " + status);
                assertEquals(TutorPostService.MSG_NOT_EDITABLE,
                        assertThrows(ConflictException.class, () -> service.submit(user, validRequest())).getMessage());
            }
            verify(postRepository, never()).saveAndFlush(any());
        }

        @Test
        void updatingApprovedPostCreatesNewPendingVersionAndKeepsPublishedOne() {
            TutorPost published = post(TutorPostStatus.PUBLISHED);
            when(postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId())).thenReturn(Optional.of(published));
            when(postRepository.findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(tutor.getTutorId(), TutorPostStatus.PUBLISHED))
                    .thenReturn(Optional.of(published));

            assertTrue(service.getMine(user).editable());
            service.submit(user, validRequest());

            ArgumentCaptor<TutorPost> saved = ArgumentCaptor.forClass(TutorPost.class);
            verify(postRepository).saveAndFlush(saved.capture());
            assertNotSame(published, saved.getValue(), "bản cập nhật là dòng mới");
            assertEquals(TutorPostStatus.PENDING_REVIEW, saved.getValue().getStatus());
            assertEquals(TutorPostStatus.PUBLISHED, published.getStatus(), "bản đang hiển thị giữ nguyên");
        }

        @Test
        void canUpdateAgainAfterUpdateRejectedWhilePublishedVersionRemains() {
            TutorPost published = post(TutorPostStatus.PUBLISHED);
            when(postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId()))
                    .thenReturn(Optional.of(post(TutorPostStatus.REJECTED)));
            when(postRepository.findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(tutor.getTutorId(), TutorPostStatus.PUBLISHED))
                    .thenReturn(Optional.of(published));

            assertTrue(service.getMine(user).editable());
            service.submit(user, validRequest());
            verify(postRepository).saveAndFlush(any());
        }

        @Test
        void submittedMessageDistinguishesFirstPostFromUpdate() {
            assertEquals(TutorPostService.MSG_SUBMITTED, TutorPostService.submittedMessage(null));
            when(postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId()))
                    .thenReturn(Optional.of(post(TutorPostStatus.PUBLISHED)));
            when(postRepository.findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(tutor.getTutorId(), TutorPostStatus.PUBLISHED))
                    .thenReturn(Optional.of(post(TutorPostStatus.PUBLISHED)));
            assertEquals(TutorPostService.MSG_UPDATE_SUBMITTED,
                    TutorPostService.submittedMessage(service.getMine(user).post()));
        }

        @Test
        void resubmitAfterRevisionRequestReusesPostAndClearsReview() {
            TutorPost revision = post(TutorPostStatus.NEEDS_REVISION);
            revision.setRejectionReason("Bổ sung kinh nghiệm");
            revision.setReviewedBy(officer);
            when(postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId())).thenReturn(Optional.of(revision));

            assertTrue(service.getMine(user).editable());
            service.submit(user, validRequest());

            verify(postRepository).saveAndFlush(revision);
            assertEquals(TutorPostStatus.PENDING_REVIEW, revision.getStatus());
            assertNull(revision.getRejectionReason());
            assertNull(revision.getReviewedBy());
            assertNull(revision.getReviewedAt());
        }
    }

    /** UC-129, UC-130 (2026-11-24): mỗi quyết định luôn gửi gia sư cả email và thông báo trong ứng dụng. */
    @Nested
    class ReviewAlwaysSendsEmailAndInAppNotification {
        TutorPostMailService mail = mock(TutorPostMailService.class);
        NotificationEvents events = mock(NotificationEvents.class);
        TutorPost pending = post(TutorPostStatus.PENDING_REVIEW);

        @BeforeEach
        void wire() {
            ReflectionTestUtils.setField(service, "mailService", mail);
            ReflectionTestUtils.setField(service, "notificationEvents", events);
            when(postRepository.findLockedById(pending.getPostId())).thenReturn(Optional.of(pending));
        }

        private void verifyInApp(String title) {
            verify(events).publish(anyString(), eq("SYSTEM"), eq(title), anyString(), eq("/tutor/post"),
                    eq(List.of(user.getUserId())));
        }

        @Test
        void approve() {
            service.approve(pending.getPostId(), officer);
            verify(mail).approvedAfterCommit("gs@kizuna.test", "Gia sư A", "Luyện thi JLPT N3 cấp tốc");
            verifyInApp("Bài đăng đã được duyệt");
        }

        @Test
        void reject() {
            service.reject(pending.getPostId(), officer, new RejectPostRequest("Nội dung không phù hợp"));
            verify(mail).rejectedAfterCommit("gs@kizuna.test", "Gia sư A", "Luyện thi JLPT N3 cấp tốc", "Nội dung không phù hợp");
            verifyInApp("Bài đăng không được duyệt");
        }

        @Test
        void requestRevision() {
            service.requestInfo(pending.getPostId(), officer, new RejectPostRequest("Bổ sung lộ trình học"));
            verify(mail).revisionRequestedAfterCommit("gs@kizuna.test", "Gia sư A", "Luyện thi JLPT N3 cấp tốc", "Bổ sung lộ trình học");
            verifyInApp("Cần bổ sung bài đăng");
        }

        @Test
        void invalidInputSendsNothing() {
            assertThrows(BadRequestException.class, () -> service.reject(pending.getPostId(), officer, new RejectPostRequest(" ")));
            verifyNoInteractions(mail, events);
        }
    }

    @Nested
    class OfficerSide {

        @Test
        void approvePublishesAndSyncsTutorSearchFields() {
            TutorPost pending = post(TutorPostStatus.PENDING_REVIEW);
            when(postRepository.findLockedById(pending.getPostId())).thenReturn(Optional.of(pending));

            service.approve(pending.getPostId(), officer);

            assertEquals(TutorPostStatus.PUBLISHED, pending.getStatus());
            assertSame(officer, pending.getReviewedBy());
            assertNotNull(pending.getReviewedAt());
            assertEquals(new BigDecimal("250000"), tutor.getHourlyRate());
            assertEquals("JLPT N3", tutor.getHeadline());
            verify(tutorRepository).save(tutor);
        }

        @Test
        void requestInfoNeedsMessageAndAllowsTutorToEdit() {
            TutorPost pending = post(TutorPostStatus.PENDING_REVIEW);
            when(postRepository.findLockedById(pending.getPostId())).thenReturn(Optional.of(pending));

            assertThrows(BadRequestException.class, () -> service.requestInfo(pending.getPostId(), officer, new RejectPostRequest("  ")));
            service.requestInfo(pending.getPostId(), officer, new RejectPostRequest("Bổ sung lộ trình học"));

            assertEquals(TutorPostStatus.NEEDS_REVISION, pending.getStatus());
            assertEquals("Bổ sung lộ trình học", pending.getRejectionReason());
        }

        @Test
        void rejectNeedsReasonAndIsFinal() {
            TutorPost pending = post(TutorPostStatus.PENDING_REVIEW);
            when(postRepository.findLockedById(pending.getPostId())).thenReturn(Optional.of(pending));

            assertThrows(BadRequestException.class, () -> service.reject(pending.getPostId(), officer, null));
            service.reject(pending.getPostId(), officer, new RejectPostRequest("Nội dung không phù hợp"));

            assertEquals(TutorPostStatus.REJECTED, pending.getStatus());
            assertEquals("Nội dung không phù hợp", pending.getRejectionReason());
        }

        @Test
        void onlyPendingPostsCanBeReviewed() {
            TutorPost published = post(TutorPostStatus.PUBLISHED);
            when(postRepository.findLockedById(published.getPostId())).thenReturn(Optional.of(published));

            assertEquals(TutorPostService.MSG_ALREADY_REVIEWED,
                    assertThrows(ConflictException.class, () -> service.approve(published.getPostId(), officer)).getMessage());
            assertEquals(TutorPostService.MSG_ALREADY_REVIEWED, assertThrows(ConflictException.class,
                    () -> service.requestInfo(published.getPostId(), officer, new RejectPostRequest("x"))).getMessage());
            verify(postRepository, never()).save(any());
        }

        @Test
        void unknownStatusFilterIsRejected() {
            assertThrows(BadRequestException.class, () -> service.list("KHONG_HOP_LE", null, 0, 10));
        }

        // ----- REQ-12: giá trị biên khi duyệt bài -----

        private TutorPost pendingPost() {
            TutorPost pending = post(TutorPostStatus.PENDING_REVIEW);
            when(postRepository.findLockedById(pending.getPostId())).thenReturn(Optional.of(pending));
            return pending;
        }

        @Test
        void req12_rejectReasonOf2000CharactersIsAccepted() {
            TutorPost pending = pendingPost();

            service.reject(pending.getPostId(), officer, new RejectPostRequest("r".repeat(2000)));

            assertEquals(TutorPostStatus.REJECTED, pending.getStatus());
            assertEquals("r".repeat(2000), pending.getRejectionReason());
        }

        @Test
        void req12_rejectReasonOf2001CharactersIsRejectedAndPostStaysPending() {
            TutorPost pending = pendingPost();

            BadRequestException e = assertThrows(BadRequestException.class,
                    () -> service.reject(pending.getPostId(), officer, new RejectPostRequest("r".repeat(2001))));

            assertEquals("Lý do từ chối tối đa 2.000 ký tự.", e.getMessage());
            assertEquals(TutorPostStatus.PENDING_REVIEW, pending.getStatus());
            verify(postRepository, never()).findLockedById(any());
            verify(postRepository, never()).save(any());
        }

        @Test
        void req12_revisionRequestOf2000CharactersIsAcceptedAnd2001IsRejected() {
            TutorPost pending = pendingPost();

            BadRequestException e = assertThrows(BadRequestException.class,
                    () -> service.requestInfo(pending.getPostId(), officer, new RejectPostRequest("m".repeat(2001))));
            assertEquals("Nội dung yêu cầu tối đa 2.000 ký tự.", e.getMessage());
            assertEquals(TutorPostStatus.PENDING_REVIEW, pending.getStatus());

            service.requestInfo(pending.getPostId(), officer, new RejectPostRequest("m".repeat(2000)));
            assertEquals(TutorPostStatus.NEEDS_REVISION, pending.getStatus());
        }

        @Test
        void req12_alreadyRejectedPostCannotBeReviewedAgain() {
            TutorPost rejected = post(TutorPostStatus.REJECTED);
            when(postRepository.findLockedById(rejected.getPostId())).thenReturn(Optional.of(rejected));

            assertEquals(TutorPostService.MSG_ALREADY_REVIEWED, assertThrows(ConflictException.class,
                    () -> service.reject(rejected.getPostId(), officer, new RejectPostRequest("Lý do"))).getMessage());
            assertEquals(TutorPostStatus.REJECTED, rejected.getStatus());
            verify(tutorRepository, never()).save(any());
        }

        @Test
        void req12_searchKeywordOf100CharactersIsAccepted() {
            when(postRepository.search(any(), any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

            assertDoesNotThrow(() -> service.list(null, "k".repeat(100), 0, 10));
        }

        @Test
        void req12_searchKeywordOf101CharactersIsRejected() {
            BadRequestException e = assertThrows(BadRequestException.class,
                    () -> service.list(null, "k".repeat(101), 0, 10));

            assertEquals("Từ khóa tìm kiếm tối đa 100 ký tự.", e.getMessage());
            verify(postRepository, never()).search(any(), any(), any());
        }
    }

    /** REQ-10: giá trị biên của nội dung bài đăng. */
    @Nested
    class ContentBoundaries {

        private static final String TITLE = "Luyện thi JLPT N3 cấp tốc";
        private static final String DESCRIPTION = "Giới thiệu ".repeat(10);
        private static final List<PriceItemRequest> PRICES =
                List.of(new PriceItemRequest("JLPT_N3", 90, new BigDecimal("250000")));

        private void assertAccepted(SubmitPostRequest request) {
            service.submit(user, request);
            ArgumentCaptor<TutorPost> saved = ArgumentCaptor.forClass(TutorPost.class);
            verify(postRepository).saveAndFlush(saved.capture());
            assertEquals(TutorPostStatus.PENDING_REVIEW, saved.getValue().getStatus());
        }

        private void assertRejected(SubmitPostRequest request, String expectedError) {
            BadRequestException e = assertThrows(BadRequestException.class, () -> service.submit(user, request));
            assertTrue(e.getMessage().contains(expectedError), e.getMessage());
            verify(postRepository, never()).saveAndFlush(any());
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {10, 11, 149, 150})
        void req10_titleWithinTenTo150CharactersIsAccepted(int length) {
            assertAccepted(new SubmitPostRequest("t".repeat(length), DESCRIPTION, null, "online", PRICES));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {9, 151})
        void req10_titleOutsideTenTo150CharactersIsRejected(int length) {
            assertRejected(new SubmitPostRequest("t".repeat(length), DESCRIPTION, null, "online", PRICES),
                    "Tiêu đề từ 10 đến 150 ký tự");
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {50, 51, 2999, 3000})
        void req10_introductionWithin50To3000CharactersIsAccepted(int length) {
            assertAccepted(new SubmitPostRequest(TITLE, "d".repeat(length), null, "online", PRICES));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {49, 3001})
        void req10_introductionOutside50To3000CharactersIsRejected(int length) {
            assertRejected(new SubmitPostRequest(TITLE, "d".repeat(length), null, "online", PRICES),
                    "Nội dung giới thiệu từ 50 đến 3.000 ký tự");
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {254, 255})
        void req10_expertiseUpTo255CharactersIsAccepted(int length) {
            assertAccepted(new SubmitPostRequest(TITLE, DESCRIPTION, "s".repeat(length), "online", PRICES));
        }

        @Test
        void req10_expertiseOf256CharactersIsRejected() {
            assertRejected(new SubmitPostRequest(TITLE, DESCRIPTION, "s".repeat(256), "online", PRICES),
                    "Chuyên môn tối đa 255 ký tự");
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.NullAndEmptySource
        @org.junit.jupiter.params.provider.ValueSource(strings = {"  ", "QUA_THU"})
        void req10_teachingFormatIsRequired(String format) {
            assertRejected(new SubmitPostRequest(TITLE, DESCRIPTION, null, format, PRICES), "Vui lòng chọn hình thức dạy");
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"10000", "10001", "9999999", "10000000"})
        void req10_tuitionWithinTenThousandToTenMillionIsAccepted(String rate) {
            assertAccepted(requestWith(List.of(new PriceItemRequest("JLPT_N3", 90, new BigDecimal(rate)))));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"9999", "10000001"})
        void req10_tuitionOutsideTenThousandToTenMillionIsRejected(String rate) {
            assertRejected(requestWith(List.of(new PriceItemRequest("JLPT_N3", 90, new BigDecimal(rate)))),
                    "Học phí mỗi buổi từ 10.000 đ đến 10.000.000 đ");
        }

        @Test
        void req10_missingTuitionAmountIsRejected() {
            assertRejected(requestWith(java.util.Collections.singletonList(new PriceItemRequest("JLPT_N3", 90, null))),
                    "Học phí mỗi buổi từ 10.000 đ đến 10.000.000 đ");
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {60, 90, 120})
        void req10_sessionLengthOf60_90_120MinutesIsAccepted(int minutes) {
            assertAccepted(requestWith(List.of(new PriceItemRequest("JLPT_N3", minutes, new BigDecimal("250000")))));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {0, 45, 59, 61, 150})
        void req10_otherSessionLengthsAreRejected(int minutes) {
            assertRejected(requestWith(List.of(new PriceItemRequest("JLPT_N3", minutes, new BigDecimal("250000")))),
                    "Vui lòng chọn thời lượng mỗi buổi (60, 90 hoặc 120 phút) cho từng môn học");
        }
    }
}
