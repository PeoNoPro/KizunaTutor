package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-6.1.3: hình thức học của lớp. Online: link không bắt buộc, có thì phải http(s). Trực tiếp: bắt buộc địa điểm.
 * Đặt trong cùng package với ClassroomServiceImpl để gọi trực tiếp quy tắc applyDelivery.
 */
class ClassroomDeliveryRulesTest {

    private static void assertField(Runnable call, String field) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ClassroomException.class,
                e -> assertThat(e.getFieldErrors()).containsKey(field));
    }

    @Test
    void onlineClassLinkIsOptionalButMustBeHttpOrHttps() {
        LearningSpace s = new LearningSpace();
        ClassroomServiceImpl.applyDelivery(s, "ONLINE", "  ", null);
        assertThat(s.getLearningMode()).isEqualTo("ONLINE");
        assertThat(s.getMeetingLink()).isNull();

        ClassroomServiceImpl.applyDelivery(s, "online", " https://meet.google.com/abc-defg-hij ", "bỏ qua");
        assertThat(s.getMeetingLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(s.getLocationDetails()).as("lớp online không giữ địa điểm").isNull();
        ClassroomServiceImpl.applyDelivery(s, "ONLINE", "HTTP://zoom.us/j/1", null);
        assertThat(s.getMeetingLink()).isEqualTo("HTTP://zoom.us/j/1");

        assertField(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "ONLINE", "meet.google.com/abc", null), "meetingLink");
        assertField(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "ONLINE", "ftp://x.vn/a", null), "meetingLink");
        assertField(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "ONLINE", "https://x.vn/" + "a".repeat(500), null),
                "meetingLink");
    }

    @Test
    void blankModeMeansOnline() {
        LearningSpace s = new LearningSpace();
        ClassroomServiceImpl.applyDelivery(s, null, null, null);
        assertThat(s.getLearningMode()).isEqualTo("ONLINE");
        assertField(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "HYBRID", null, null), "learningMode");
    }

    @Test
    void offlineClassRequiresLocationUpTo500CharsAndDropsTheLink() {
        LearningSpace s = new LearningSpace();
        String address = "12 Lê Lợi, Phường Bến Thành, Thành phố Hồ Chí Minh";
        ClassroomServiceImpl.applyDelivery(s, "OFFLINE", "https://meet.google.com/x", "  " + address + "  ");
        assertThat(s.getLearningMode()).isEqualTo("OFFLINE");
        assertThat(s.getLocationDetails()).isEqualTo(address);
        assertThat(s.getMeetingLink()).as("lớp trực tiếp không giữ link").isNull();

        assertField(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "OFFLINE", null, "   "), "locationDetails");
        assertField(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "OFFLINE", null, null), "locationDetails");
        ClassroomServiceImpl.applyDelivery(new LearningSpace(), "OFFLINE", null, "a".repeat(500));
        assertField(() -> ClassroomServiceImpl.applyDelivery(new LearningSpace(), "OFFLINE", null, "a".repeat(501)), "locationDetails");
    }
}
