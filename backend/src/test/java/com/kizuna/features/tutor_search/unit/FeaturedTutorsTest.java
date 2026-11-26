package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.tutor_post.entity.TutorPost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-05 / UC-05.04 View Featured Tutors - TutorSearchServiceImpl.getFeaturedTutors().
 * Lấy tối đa 6 gia sư nổi bật theo điểm; chỉ giữ người có bài đăng đã duyệt và gói hội viên còn hạn.
 */
class FeaturedTutorsTest extends TutorSearchUnitTestBase {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000f2");

    private static TutorPost post(Tutor t) { return TutorPost.builder().tutor(t).title("Post of " + t.getUser().getFullName()).build(); }

    @Test @DisplayName("UTCID01: Gia sư nổi bật có bài đăng và gói còn hạn -> hiển thị theo thứ tự điểm")
    void utcid01_featured() {
        Tutor a = tutor(A, "Tutor A", "Hà Nội"), b = tutor(B, "Tutor B", "Hà Nội");
        when(repo.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc()).thenReturn(List.of(a, b));
        when(posts.publishedPosts(any())).thenReturn(Map.of(A, post(a), B, post(b)));
        when(posts.hasActiveMembership(any(), any())).thenReturn(true);

        var result = service.getFeaturedTutors();

        assertEquals(List.of("Tutor A", "Tutor B"), result.stream().map(r -> r.getFullName()).toList());
    }

    @Test @DisplayName("UTCID02: Giới hạn 6 gia sư lấy từ truy vấn top 6")
    void utcid02_top6() {
        when(repo.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc()).thenReturn(List.of());
        service.getFeaturedTutors();
        verify(repo).findTop6ByIsFeaturedTrueOrderByAvgRatingDesc();
        verify(repo, never()).findAll();
    }

    @Test @DisplayName("UTCID03: Gia sư nổi bật chưa có bài đăng đã duyệt -> bị ẩn")
    void utcid03_noPublishedPost() {
        Tutor a = tutor(A, "Tutor A", "Hà Nội"), b = tutor(B, "Tutor B", "Hà Nội");
        when(repo.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc()).thenReturn(List.of(a, b));
        when(posts.publishedPosts(any())).thenReturn(Map.of(A, post(a)));
        when(posts.hasActiveMembership(any(), any())).thenReturn(true);

        var result = service.getFeaturedTutors();

        assertEquals(1, result.size());
        assertEquals(A, result.get(0).getTutorId());
    }

    @Test @DisplayName("UTCID04: Gia sư nổi bật hết gói hội viên -> bị ẩn")
    void utcid04_noMembership() {
        Tutor a = tutor(A, "Tutor A", "Hà Nội"), b = tutor(B, "Tutor B", "Hà Nội");
        when(repo.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc()).thenReturn(List.of(a, b));
        when(posts.publishedPosts(any())).thenReturn(Map.of(A, post(a), B, post(b)));
        when(posts.hasActiveMembership(eq(A), any())).thenReturn(true);
        when(posts.hasActiveMembership(eq(B), any())).thenReturn(false);

        assertEquals(List.of(A), service.getFeaturedTutors().stream().map(r -> r.getTutorId()).toList());
    }

    @Test @DisplayName("UTCID05: Không có gia sư nổi bật hiển thị được -> danh sách rỗng")
    void utcid05_none() {
        when(repo.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc()).thenReturn(List.of());
        assertTrue(service.getFeaturedTutors().isEmpty());
    }
}
