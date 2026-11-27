package com.kizuna.features.booking.service.impl;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.entity.TutorPostStatus;
import com.kizuna.features.tutor_post.repository.TutorPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Hình thức học của yêu cầu kết nối phải nằm trong hình thức dạy của bài đăng gia sư. */
class ConnectionLearningModeTest {

    private final UUID tutorId = UUID.randomUUID();

    private ConnectionRequestServiceImpl serviceWithFormat(String format) {
        ConnectionRequestServiceImpl service = new ConnectionRequestServiceImpl(
                null, null, null, null, null, null, null, null, null, null);
        TutorPostRepository posts = mock(TutorPostRepository.class);
        TutorPost post = new TutorPost();
        post.setTeachingFormat(format);
        when(posts.findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(any(), eq(TutorPostStatus.PUBLISHED)))
                .thenReturn(format == null ? Optional.empty() : Optional.of(post));
        ReflectionTestUtils.setField(service, "tutorPostRepository", posts);
        return service;
    }

    @Test
    void bothAllowsEitherMode() {
        ConnectionRequestServiceImpl service = serviceWithFormat("BOTH");
        assertThat(service.resolveLearningMode(tutorId, null)).isEqualTo("ONLINE");
        assertThat(service.resolveLearningMode(tutorId, "offline")).isEqualTo("OFFLINE");
    }

    @Test
    void singleFormatOnlyAllowsThatMode() {
        assertThatThrownBy(() -> serviceWithFormat("ONLINE").resolveLearningMode(tutorId, "OFFLINE"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> serviceWithFormat("OFFLINE").resolveLearningMode(tutorId, "ONLINE"))
                .isInstanceOf(BadRequestException.class);
        assertThat(serviceWithFormat("OFFLINE").resolveLearningMode(tutorId, "OFFLINE")).isEqualTo("OFFLINE");
    }

    @Test
    void noPublishedFormatMeansOnlineOnly() {
        ConnectionRequestServiceImpl service = serviceWithFormat(null);
        assertThat(service.resolveLearningMode(tutorId, "")).isEqualTo("ONLINE");
        assertThatThrownBy(() -> service.resolveLearningMode(tutorId, "OFFLINE")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.resolveLearningMode(tutorId, "HYBRID")).isInstanceOf(BadRequestException.class);
    }
}
