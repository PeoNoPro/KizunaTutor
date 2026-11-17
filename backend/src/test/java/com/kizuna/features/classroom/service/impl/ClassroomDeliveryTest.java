package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Hình thức học của lớp (migration 029): online + link, hoặc trực tiếp + địa điểm. */
class ClassroomDeliveryTest {

    @Test
    void blankModeMeansOnlineAndDropsLocation() {
        LearningSpace space = new LearningSpace();
        ClassroomServiceImpl.applyDelivery(space, null, " https://meet.google.com/abc ", "Cầu Giấy");
        assertThat(space.getLearningMode()).isEqualTo("ONLINE");
        assertThat(space.getMeetingLink()).isEqualTo("https://meet.google.com/abc");
        assertThat(space.getLocationDetails()).isNull();
    }

    @Test
    void onlineLinkIsOptionalButMustBeHttp() {
        LearningSpace space = new LearningSpace();
        ClassroomServiceImpl.applyDelivery(space, "online", "", null);
        assertThat(space.getMeetingLink()).isNull();
        assertThatThrownBy(() -> ClassroomServiceImpl.applyDelivery(space, "ONLINE", "meet.google.com/abc", null))
                .isInstanceOf(ClassroomException.class);
        assertThatThrownBy(() -> ClassroomServiceImpl.applyDelivery(space, "ONLINE", "javascript:alert(1)", null))
                .isInstanceOf(ClassroomException.class);
    }

    @Test
    void offlineRequiresLocationAndDropsLink() {
        LearningSpace space = new LearningSpace();
        assertThatThrownBy(() -> ClassroomServiceImpl.applyDelivery(space, "OFFLINE", null, "  "))
                .isInstanceOf(ClassroomException.class);
        ClassroomServiceImpl.applyDelivery(space, "OFFLINE", "https://meet.google.com/abc", "Cầu Giấy, Hà Nội");
        assertThat(space.getLearningMode()).isEqualTo("OFFLINE");
        assertThat(space.getLocationDetails()).isEqualTo("Cầu Giấy, Hà Nội");
        assertThat(space.getMeetingLink()).isNull();
    }

    @Test
    void unknownModeRejected() {
        assertThatThrownBy(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "HYBRID", null, null))
                .isInstanceOf(ClassroomException.class);
    }
}
