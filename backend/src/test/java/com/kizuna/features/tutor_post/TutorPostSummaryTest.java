package com.kizuna.features.tutor_post;

import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.payment.service.TutorMembershipService;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.ReviewSummaryView;
import com.kizuna.features.tutor_post.entity.TutorPostStatus;
import com.kizuna.features.tutor_post.repository.TutorPostRepository;
import com.kizuna.features.tutor_post.service.TutorPostService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * REQ-28 (UC-31): số bài đăng giới thiệu theo trạng thái trên trang tổng quan của cán bộ học vụ.
 */
class TutorPostSummaryTest {

    TutorPostRepository postRepository = mock(TutorPostRepository.class);
    TutorPostService service = new TutorPostService(postRepository, mock(TutorRepository.class),
            mock(TutorMembershipService.class), mock(UserRepository.class));

    @Test
    void summaryCountsPostsByReviewStatus() {
        when(postRepository.countByStatus(TutorPostStatus.PENDING_REVIEW)).thenReturn(7L);
        when(postRepository.countByStatus(TutorPostStatus.NEEDS_REVISION)).thenReturn(2L);
        when(postRepository.countByStatus(TutorPostStatus.PUBLISHED)).thenReturn(15L);
        when(postRepository.countByStatus(TutorPostStatus.REJECTED)).thenReturn(1L);

        ReviewSummaryView summary = service.summary();

        assertEquals(7, summary.pendingCount());
        assertEquals(2, summary.needsInfoCount());
        assertEquals(15, summary.approvedCount());
        assertEquals(1, summary.rejectedCount());
    }

    @Test
    void noPostShowsAllCountsAsZero() {
        ReviewSummaryView summary = service.summary();

        assertEquals(0, summary.pendingCount());
        assertEquals(0, summary.needsInfoCount());
        assertEquals(0, summary.approvedCount());
        assertEquals(0, summary.rejectedCount());
        verify(postRepository, times(4)).countByStatus(any());
    }
}
